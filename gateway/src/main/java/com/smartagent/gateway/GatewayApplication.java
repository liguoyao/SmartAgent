package com.smartagent.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway Application
 * API Gateway with JWT authentication, rate limiting, and service routing
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.smartagent.common", "com.smartagent.gateway"})
@MapperScan("com.smartagent.gateway.mapper")
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
