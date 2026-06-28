package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.CollegeBasic;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CollegeBasicMapper extends BaseMapper<CollegeBasic> {

    /**
     * 根据批次随机获取院校
     */
    List<CollegeBasic> selectRandomByBatch(String admissionBatch);
}
