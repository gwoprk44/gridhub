package com.gridhub.f1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GridhubApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridhubApplication.class, args);
    }

}
