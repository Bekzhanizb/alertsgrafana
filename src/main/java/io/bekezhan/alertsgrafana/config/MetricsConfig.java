package io.bekezhan.alertsgrafana.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Micrometer configuration.
 *
 * TimedAspect — enables @Timed on arbitrary Spring beans (not just HTTP handlers).
 * Without this bean, @Timed only works on @RestController methods via the filter.
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}