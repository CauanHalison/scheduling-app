package com.github.cauanHalison.scheduling_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class SchedulingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedulingApiApplication.class, args);
	}

}
