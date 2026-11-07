package com.shop.user.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.shop.common.security.JwtProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@EnableCaching
public class BeansConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    /** Only user-service can ISSUE tokens; all others only hold a JwtDecoder. */
    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        var key = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /** Cache-aside with a TTL: bounds staleness even if an eviction is missed. */
    @Bean
    RedisCacheManagerBuilderCustomizer cacheTtl() {
        return b -> b.withCacheConfiguration("users",
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)).disableCachingNullValues());
    }
}
