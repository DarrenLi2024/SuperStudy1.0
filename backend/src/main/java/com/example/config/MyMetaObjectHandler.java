package com.example.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器
 * 兼容 Date 和 LocalDateTime 两种字段类型
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        fillCreatedAt(metaObject);
        fillUpdatedAt(metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        fillUpdatedAt(metaObject);
    }

    private void fillCreatedAt(MetaObject metaObject) {
        if (metaObject.hasSetter("createdAt")) {
            Class<?> setterType = metaObject.getSetterType("createdAt");
            if (LocalDateTime.class.equals(setterType)) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
            } else if (Date.class.equals(setterType)) {
                this.strictInsertFill(metaObject, "createdAt", Date.class, new Date());
            }
        }
    }

    private void fillUpdatedAt(MetaObject metaObject) {
        if (metaObject.hasSetter("updatedAt")) {
            Class<?> setterType = metaObject.getSetterType("updatedAt");
            if (LocalDateTime.class.equals(setterType)) {
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            } else if (Date.class.equals(setterType)) {
                this.strictInsertFill(metaObject, "updatedAt", Date.class, new Date());
            }
        }
    }
}
