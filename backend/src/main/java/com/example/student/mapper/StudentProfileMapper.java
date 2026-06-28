package com.example.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.student.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
}
