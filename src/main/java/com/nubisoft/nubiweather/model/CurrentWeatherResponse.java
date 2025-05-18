package com.nubisoft.nubiweather.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CurrentWeatherResponse {
    private Location location;
    private Current current;

    @Data
    public static class Location {
        private String name;
        private String region;
        private String country;
        @JsonProperty("localtime_epoch")
        private long localtimeEpoch;
        private String localtime;
    }

    @Data
    public static class Current {
        @JsonProperty("temp_c")
        private double tempC;
        @JsonProperty("feelslike_c")
        private double feelsLikeC;
        private Condition condition;
        @JsonProperty("wind_kph")
        private double windKph;
        private double humidity;
    }

    @Data
    public static class Condition {
        private String text;
        private String icon;
        private int code;
    }
}
