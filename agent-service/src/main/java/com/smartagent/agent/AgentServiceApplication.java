package com.smartagent.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Agent Service Application
 * Manages AI agents including Supervisor Agent and Specialist Agents
 */
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
@MapperScan("com.smartagent.agent.repository")
public class AgentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
