package transactionService.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import transactionService.demo.security.BearerTokenPropagationInterceptor;

/**
 * Outbound HTTP clients. Every one of them carries the token-propagation interceptor, so an
 * authenticated inbound request stays authenticated across the service hop.
 */
@Configuration
@EnableConfigurationProperties(ServiceClientProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient bankingServiceRestClient(ServiceClientProperties properties,
                                               BearerTokenPropagationInterceptor tokenPropagation) {
        return RestClient.builder()
                .baseUrl(properties.bankingServiceUrl())
                .requestInterceptor(tokenPropagation)
                .build();
    }
}
