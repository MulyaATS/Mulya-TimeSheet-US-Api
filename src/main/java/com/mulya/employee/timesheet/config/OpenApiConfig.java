package com.mulya.employee.timesheet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI timesheetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Timesheet Microservice API")
                        .description("API documentation for Timesheet microservice")
                        .version("1.0.0"));
    }
}
