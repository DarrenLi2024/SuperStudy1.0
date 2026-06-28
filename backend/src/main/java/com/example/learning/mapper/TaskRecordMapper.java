package com.example.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learning.entity.TaskRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {
}
