package com.review.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@ServletComponentScan(basePackages = "com.review.agent.common.filter")
@EnableScheduling
public class ReviewAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReviewAgentApplication.class, args);
	}

}
