package com.elgourmat.careflow.config;

import com.elgourmat.careflow.adapter.in.rest.ratelimit.RateLimitFilter;
import com.elgourmat.careflow.adapter.in.rest.ratelimit.RateLimitProperties;
import com.elgourmat.careflow.adapter.in.rest.ratelimit.TokenBucketRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            TokenBucketRegistry registry, RateLimitProperties properties) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(registry, properties));
        registration.addUrlPatterns("/api/claims");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
