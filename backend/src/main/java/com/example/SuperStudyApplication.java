package com.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({"com.example.sys.mapper", "com.example.student.mapper", "com.example.exam.mapper", "com.example.growth.mapper", "com.example.learning.mapper", "com.example.mapper"})
@EnableScheduling
public class SuperStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperStudyApplication.class, args);
    }
}
