package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    CourseBaseMapper courseBaseMapper;
    @Resource
    CourseMarketMapper courseMarketMapper;
    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseMarketServiceImpl courseMarketService;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {

        //构建条件查询对象
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        //根据课程名称查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()), CourseBase::getName, queryCourseParamsDto.getCourseName());
        //根据课程审核状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()), CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());
        //根据课程发布状态
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()), CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());

        //分页对象(参数为当前页和每页记录数)
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //查询分页结果
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);

        //获取数据列表
        List<CourseBase> list = pageResult.getRecords();

        //获取数据总数
        long total = pageResult.getTotal();

        //构建结果集
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());

        return courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {

//        //合法性校验
//        if (StringUtils.isBlank(dto.getName())) {
//            XueChengPlusException.cast("课程名称为空");
//        }
//
//        if (StringUtils.isBlank(dto.getMt())) {
//            XueChengPlusException.cast("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getSt())) {
//            XueChengPlusException.cast("课程分类为空");
//        }
//
//        if (StringUtils.isBlank(dto.getGrade())) {
//            XueChengPlusException.cast("课程等级为空");
//        }
//
//        if (StringUtils.isBlank(dto.getTeachmode())) {
//            XueChengPlusException.cast("教育模式为空");
//        }
//
//        if (StringUtils.isBlank(dto.getUsers())) {
//            XueChengPlusException.cast("适应人群为空");
//        }
//
//        if (StringUtils.isBlank(dto.getCharge())) {
//            XueChengPlusException.cast("收费规则为空");
//        }

        //创建新增课程对象
        CourseBase courseBaseNew = new CourseBase();
        //将前端传来的信息赋值给新对象
        BeanUtils.copyProperties(dto, courseBaseNew);
        //设置审核状态
        courseBaseNew.setAuditStatus("202002");
        //设置发布状态
        courseBaseNew.setStatus("203001");
        //机构id
        courseBaseNew.setCompanyId(companyId);
        //添加时间
        courseBaseNew.setCreateDate(LocalDateTime.now());
        //将构建完成的新课程对象插入课程基本信息表
        int insert = courseBaseMapper.insert(courseBaseNew);
        Long courseId = courseBaseNew.getId();
        //课程营销信息表
        //将dto中和courseMarket属性名一样的属性值拷贝到courseMarket
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(dto, courseMarketNew);
        //设置课程id
        courseMarketNew.setId(courseId);

        //如果课程收费，价格必须有条件输入
        String charge = dto.getCharge();
        if (charge.equals("201001")) {//收费
            if (courseMarketNew.getPrice() == null || courseMarketNew.getPrice().floatValue() <= 0) {
                XueChengPlusException.cast("课程为收费，请输入价格且>0");
            }
        }

        //插入课程营销表一条记录
        int insert1 = courseMarketMapper.insert(courseMarketNew);

        if (insert1 <= 0 || insert <= 0) {
            throw new XueChengPlusException("添加课程失败");
        }

        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);
        return courseBaseInfo;
    }

    /**
     * 根据课程id查询课程基本信息和营销信息
     *
     * @param courseId
     * @return
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {

        //基本信息
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);

        //如果营销信息不为空，则拷贝
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }

        //查询出分类名称
        String mt = courseBase.getMt();
        String st = courseBase.getSt();
        CourseCategory mtCategory = courseCategoryMapper.selectById(mt);
        CourseCategory stCategory = courseCategoryMapper.selectById(st);

        if (mtCategory != null) {
            //分类名称
            String mtName = mtCategory.getName();
            courseBaseInfoDto.setMt(mtName);
        }
        if (stCategory != null) {
            //分类名称
            String stName = stCategory.getName();
            courseBaseInfoDto.setMt(stName);
        }

        return courseBaseInfoDto;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {

        //获得当前要修改的课程id
        Long courseId = editCourseDto.getId();
        //查询出当前课程
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XueChengPlusException.cast("课程不存在");
        }
        //判断是否由本机构修改
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("课程信息只能由发布机构修改");
        }
        //将基本信息拷贝
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //数据库更新课程基本信息表
        courseBase.setChangeDate(LocalDateTime.now());//设置修改时间
        courseBaseMapper.updateById(courseBase);

//        //查询营销信息
//        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
//        if (courseMarket == null) {
//            courseMarket = new CourseMarket();
//        }
        CourseMarket courseMarket = new CourseMarket();

//        String charge = editCourseDto.getCharge();
//        courseMarket.setCharge(charge);
//        //收费课程必须写价格
//        if (charge.equals("201001")) {
//            Float price = editCourseDto.getPrice();
//            if (price == null || price.floatValue() <= 0) {
//                XueChengPlusException.cast("课程设置了收费价格不能为空且必须大于0");
//            }
//        }

        BeanUtils.copyProperties(editCourseDto, courseMarket);

        saveCourseMarket(courseMarket);

        return getCourseBaseInfo(courseId);
    }

    private int saveCourseMarket(CourseMarket courseMarket) {

        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge)) {
            XueChengPlusException.cast("请设置收费规则");
        }

        if (charge.equals("201001")) {
            Float price = courseMarket.getPrice();
            if (price == null || price <= 0) {
                XueChengPlusException.cast("课程设置了收费价格不能为空且必须大于0");
            }
        }

        boolean b = courseMarketService.saveOrUpdate(courseMarket);

        return b ? 1 : -1;

    }

}
