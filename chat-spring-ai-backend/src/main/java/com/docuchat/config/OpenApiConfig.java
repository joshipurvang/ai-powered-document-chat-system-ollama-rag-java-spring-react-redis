package com.docuchat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * <p>Provides interactive API documentation accessible at /swagger-ui.html</p>
 *
 * 
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates OpenAPI documentation bean.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI documentChatOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Document Chat AI API")
                        .description("RAG-based document chat system with Ollama integration")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Document Chat AI Team")
                                .email("support@docuchat.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }


}