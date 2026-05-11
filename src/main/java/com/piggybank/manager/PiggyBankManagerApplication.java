package com.piggybank.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.piggybank.manager.mapper")
@SpringBootApplication
public class PiggyBankManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PiggyBankManagerApplication.class, args);
    }
}
