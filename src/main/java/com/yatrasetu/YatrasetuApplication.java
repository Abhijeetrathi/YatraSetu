package com.yatrasetu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YatrasetuApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                YatrasetuApplication.class,
                args
        );

        System.out.println();
        System.out.println("==============================================");
        System.out.println("🚆 YatraSetu Railway Reservation System");
        System.out.println("==============================================");
        System.out.println("🌐 Open in browser:");
        System.out.println("http://localhost:8081/");
        System.out.println();
    }
}