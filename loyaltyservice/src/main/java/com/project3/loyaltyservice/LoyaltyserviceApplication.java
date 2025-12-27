package com.project3.loyaltyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({"com.project3.loyaltyservice", "com.project3.commonservice"})
public class LoyaltyserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoyaltyserviceApplication.class, args);
	}

}
