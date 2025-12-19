package com.review.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@ServletComponentScan(basePackages = "com.review.agent.common.filter")
public class ReviewAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReviewAgentApplication.class, args);
	}

}
