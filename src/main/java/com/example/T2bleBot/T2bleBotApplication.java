package com.example.T2bleBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.T2bleBot", "func"})
@EntityScan(basePackages = "db")
public class T2bleBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(T2bleBotApplication.class, args);
    }
}