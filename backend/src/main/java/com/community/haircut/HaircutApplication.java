package com.community.haircut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HaircutApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaircutApplication.class, args);
    }
}
