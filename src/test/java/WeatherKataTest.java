import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WeatherKataTest {

    private static final HttpClient HTTP_CLIENT = new HttpClientGoogle();
    private static final Forecast FORECAST = new ForecastMetaWeather(HTTP_CLIENT, "http://localhost:8090");
    private static final String MADRID_CITY_NAME = "Madrid";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options()
            .port(8090)
            .extensions(new ResponseTemplateTransformer(true))
    );

    @Test
    public void find_the_weather_of_today() throws ForecastException {

        Optional<String> prediction = FORECAST.predictWeather(ForecastRequest.builder()
                .cityName(MADRID_CITY_NAME)
                .build());

        assertThat(prediction.get(), is("Heavy Cloud"));
    }

    @Test
    public void find_the_weather_of_tomorrow() throws ForecastException {

        Optional<String> prediction = FORECAST.predictWeather(ForecastRequest.builder()
                .cityName(MADRID_CITY_NAME)
                .date(LocalDate.now().plusDays(1))
                .build());

        assertThat(prediction.get(), is("Light Cloud"));
    }

    @Test
    public void find_the_wind_of_today() throws ForecastException {

        Optional<String> prediction = FORECAST.predictWind(ForecastRequest.builder()
                .cityName(MADRID_CITY_NAME)
                .build());

        assertThat(prediction.get(), is("2.3178431052069253"));
    }

    @Test
    public void there_is_no_prediction_for_more_than_6_days() throws ForecastException {

        Optional<String> prediction = FORECAST.predictWeather(ForecastRequest.builder()
                .cityName(MADRID_CITY_NAME)
                .date(LocalDate.now().plusDays(6))
                .build());

        assertThat(prediction, is(Optional.empty()));
    }
}
