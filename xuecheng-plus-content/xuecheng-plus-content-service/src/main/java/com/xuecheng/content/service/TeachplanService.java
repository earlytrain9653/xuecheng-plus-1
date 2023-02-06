package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

/**
 * 课程计划管理接口
 */
public interface TeachplanService {

    /**
     * 查询课程计划树形结构
     * @param courseId
     * @return
     */
    List<TeachplanDto> findTeachplayTree(Long courseId);

    /**
     * 保存或修改课程计划
     * @param saveTeachplanDto
     */
    void saveTeachplan (SaveTeachplanDto saveTeachplanDto);
}
