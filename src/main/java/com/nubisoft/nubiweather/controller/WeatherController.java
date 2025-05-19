package com.nubisoft.nubiweather.controller;

import com.nubisoft.nubiweather.model.ForecastWeatherResponse;
import com.nubisoft.nubiweather.model.CurrentWeatherResponse;
import com.nubisoft.nubiweather.service.WeatherService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class WeatherController {

    private final WeatherService service;

    public WeatherController(WeatherService service) {
        this.service = service;
    }

    @GetMapping(value = "/realtime-weather", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<CurrentWeatherResponse> realtimeWeather() {
        return service.getRealtimeWeather();
    }

    @GetMapping(value = "/forecast-weather", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ForecastWeatherResponse> forecastWeather() {
        return service.getForecastWeather(3);
    }

    @GetMapping(value = "/realtime", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CurrentWeatherResponse> currentForCity(@RequestParam String city) {
        return service.getRealtimeWeather(city);
    }

    @GetMapping(value = "/forecast", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ForecastWeatherResponse> forecastForCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "3") int days) {
        return service.getForecastWeather(city, days);
    }
}
