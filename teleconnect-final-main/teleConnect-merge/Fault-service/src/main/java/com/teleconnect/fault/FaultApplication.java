package com.teleconnect.fault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Module 2.6 / 4.6 — Fault &amp; Service Request Management.
 * Runs on port 8086 with context path /teleConnect.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class FaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaultApplication.class, args);
    }
}
