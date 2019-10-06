import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WeatherKataTest {

    private static final Forecast FORECAST = new Forecast("http://localhost:8090");
    private static final String MADRID_CITY_NAME = "Madrid";
    private static final int ONE_DAY = 1000 * 60 * 60 * 24;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options()
            .port(8090)
            .extensions(new ResponseTemplateTransformer(true))
    );

    @Test
    public void find_the_weather_of_today() throws IOException {

        String prediction = FORECAST.predict(MADRID_CITY_NAME, null, false);

        assertThat(prediction, is("Heavy Cloud"));
    }

    @Test
    public void find_the_weather_of_tomorrow() throws IOException {

        Date tomorrow = new Date(new Date().getTime() + ONE_DAY);

        String prediction = FORECAST.predict(MADRID_CITY_NAME, tomorrow, false);

        assertThat(prediction, is("Light Cloud"));
    }

    @Test
    public void find_the_wind_of_today() throws IOException {

        String prediction = FORECAST.predict(MADRID_CITY_NAME, null, true);

        assertThat(prediction, is("2.3178431052069253"));
    }

    @Test
    public void there_is_no_prediction_for_more_than_6_days() throws IOException {

        Date tomorrow = new Date(new Date().getTime() + (ONE_DAY * 6));

        String prediction = FORECAST.predict(MADRID_CITY_NAME, tomorrow, false);

        assertThat(prediction, is(""));
    }
}
