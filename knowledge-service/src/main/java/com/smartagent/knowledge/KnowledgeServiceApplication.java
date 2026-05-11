package com.smartagent.knowledge;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smartagent.knowledge.repository")
public class KnowledgeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeServiceApplication.class, args);
    }
}
