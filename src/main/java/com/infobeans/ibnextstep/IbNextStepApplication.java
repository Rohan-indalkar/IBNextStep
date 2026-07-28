package com.infobeans.ibnextstep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IbNextStepApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbNextStepApplication.class, args);
    }
}