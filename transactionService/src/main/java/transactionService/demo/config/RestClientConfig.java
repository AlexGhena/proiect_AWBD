package transactionService.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import transactionService.demo.security.BearerTokenPropagationInterceptor;

import java.time.Duration;

/**
 * Outbound HTTP clients. Every one of them carries the token-propagation interceptor, so an
 * authenticated inbound request stays authenticated across the service hop.
 */
@Configuration
@EnableConfigurationProperties(ServiceClientProperties.class)
public class RestClientConfig {

    /**
     * Bounded so a stalled bankingService can't hang the caller indefinitely - the resilience4j
     * retry/circuit-breaker around this client only gets a chance to act once a call actually fails.
     */
    @Bean
    public ClientHttpRequestFactory bankingServiceRequestFactory() {
        return ClientHttpRequestFactoryBuilder.detect().build(HttpClientSettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3)));
    }

    @Bean
    public RestClient bankingServiceRestClient(ServiceClientProperties properties,
                                               BearerTokenPropagationInterceptor tokenPropagation,
                                               ClientHttpRequestFactory bankingServiceRequestFactory) {
        return RestClient.builder()
                .baseUrl(properties.bankingServiceUrl())
                .requestInterceptor(tokenPropagation)
                .requestFactory(bankingServiceRequestFactory)
                .build();
    }
}
