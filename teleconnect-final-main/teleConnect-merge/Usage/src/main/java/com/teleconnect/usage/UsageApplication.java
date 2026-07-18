package com.teleconnect.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class UsageApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsageApplication.class, args);
    }
}
