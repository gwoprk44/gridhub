package com.gridhub.gridhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@SpringBootApplication
public class GridhubApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridhubApplication.class, args);
    }

}
