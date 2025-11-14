package com.example.minitasker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniTaskerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniTaskerApplication.class, args);
    }
}
