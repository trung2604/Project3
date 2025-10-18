package com.project3.inventoryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("Microservice quản lý kho hàng với Event Sourcing và CQRS")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Project3 Team")
                                .email("project3@example.com")
                                .url("https://github.com/project3"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8003")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.project3.com/inventory")
                                .description("Production Server")
                ));
    }
}
