package bankingService.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Central defaults for list endpoints, so page sizes are never hardcoded per controller. */
@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(int defaultSize, int maxSize) {

    public PaginationProperties {
        if (defaultSize <= 0) {
            defaultSize = 20;
        }
        if (maxSize <= 0) {
            maxSize = 100;
        }
    }
}
