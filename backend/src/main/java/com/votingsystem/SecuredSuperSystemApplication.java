package com.votingsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecuredSuperSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecuredSuperSystemApplication.class, args);
	}

}
