package bankingService.demo.config;

import bankingService.demo.security.BearerTokenPropagationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Outbound HTTP clients. Every one of them carries the token-propagation interceptor, so an
 * authenticated inbound request stays authenticated across the service hop.
 */
@Configuration
@EnableConfigurationProperties(ServiceClientProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient userServiceRestClient(ServiceClientProperties properties,
                                            BearerTokenPropagationInterceptor tokenPropagation) {
        return RestClient.builder()
                .baseUrl(properties.userServiceUrl())
                .requestInterceptor(tokenPropagation)
                .build();
    }
}
