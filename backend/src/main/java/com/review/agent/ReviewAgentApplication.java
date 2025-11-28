package com.review.agent;

import com.review.agent.common.config.PythonProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(PythonProperties.class)
public class ReviewAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReviewAgentApplication.class, args);
	}

}
