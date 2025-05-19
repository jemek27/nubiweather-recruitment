package com.nubisoft.nubiweather.config;

import com.nubisoft.nubiweather.model.ForecastWeatherResponse;
import com.nubisoft.nubiweather.model.CurrentWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port) {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public ReactiveRedisTemplate<String, CurrentWeatherResponse> currentWeatherRedisTemplate(
            ReactiveRedisConnectionFactory cf) {
        Jackson2JsonRedisSerializer<CurrentWeatherResponse> serializer =
                new Jackson2JsonRedisSerializer<>(CurrentWeatherResponse.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, CurrentWeatherResponse> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, CurrentWeatherResponse> context = builder
                .value(serializer)
                .build();
        return new ReactiveRedisTemplate<>(cf, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, ForecastWeatherResponse> forecastWeatherRedisTemplate(
            ReactiveRedisConnectionFactory cf) {
        Jackson2JsonRedisSerializer<ForecastWeatherResponse> serializer =
                new Jackson2JsonRedisSerializer<>(ForecastWeatherResponse.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, ForecastWeatherResponse> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, ForecastWeatherResponse> context = builder
                .value(serializer)
                .build();
        return new ReactiveRedisTemplate<>(cf, context);
    }
}
