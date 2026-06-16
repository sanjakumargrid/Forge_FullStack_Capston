package com.talentgrid.jobposting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobPostingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobPostingServiceApplication.class, args);
    }
}
