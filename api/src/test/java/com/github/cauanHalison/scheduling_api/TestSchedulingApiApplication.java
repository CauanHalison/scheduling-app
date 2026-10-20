package com.github.cauanHalison.scheduling_api;

import org.springframework.boot.SpringApplication;

public class TestSchedulingApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(SchedulingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
