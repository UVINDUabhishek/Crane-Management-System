package com.uvindu.crane_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class CraneManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CraneManagementSystemApplication.class, args);
	}

}
