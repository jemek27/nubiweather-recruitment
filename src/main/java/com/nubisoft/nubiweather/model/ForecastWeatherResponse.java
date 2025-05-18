package com.nubisoft.nubiweather.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ForecastWeatherResponse {
    private Location location;
    private Forecast forecast;

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
    public static class Forecast {
        @JsonProperty("forecastday")
        private List<ForecastDay> forecastDays;
    }

    @Data
    public static class ForecastDay {
        private String date;
        private Day day;

        @Data
        public static class Day {
            @JsonProperty("maxtemp_c")
            private double maxTempC;
            @JsonProperty("mintemp_c")
            private double minTempC;
            private Condition condition;
        }
    }

    @Data
    public static class Condition {
        private String text;
        private String icon;
        private int code;
    }
}
