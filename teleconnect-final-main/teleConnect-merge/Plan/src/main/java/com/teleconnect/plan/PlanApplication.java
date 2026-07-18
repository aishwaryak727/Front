package com.teleconnect.plan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanApplication.class, args);
    }
}