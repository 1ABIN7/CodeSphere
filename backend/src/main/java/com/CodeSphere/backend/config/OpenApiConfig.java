package com.CodeSphere.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * 
 * Swagger UI available at:
 * - http://localhost:8080/swagger-ui.html (local)
 * - http://localhost:8080/v3/api-docs (raw JSON)
 * 
 * All @RestController classes are auto-scanned and documented.
 * P5 responsibility: keep this updated as new endpoints are added.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeSphereOpenAPI() {

        // Local development server
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development");

        // Project contact info
        Contact contact = new Contact();
        contact.setName("CodeSphere Team");
        contact.setEmail("team@CodeSphere.com");

        // API metadata — title, version, description
        Info info = new Info()
                .title("CodeSphere API")
                .version("1.0.0")
                .description("REST API documentation for CodeSphere — Online Assessment Platform")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}