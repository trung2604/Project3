package com.project3.loyaltyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableEurekaClient
@ComponentScan({"com.project3.loyaltyservice", "com.project3.commonservice"})
public class LoyaltyserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoyaltyserviceApplication.class, args);
	}

}
