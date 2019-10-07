package forecast;

import java.util.Optional;

public interface Forecast {
    Optional<String> predictWind(ForecastRequest request) throws ForecastException;
    Optional<String> predictWeather(ForecastRequest request) throws ForecastException;
}
