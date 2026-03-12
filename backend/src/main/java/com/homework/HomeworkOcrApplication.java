package com.homework;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.homework.mapper")
@EnableScheduling
public class HomeworkOcrApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomeworkOcrApplication.class, args);
    }
}
