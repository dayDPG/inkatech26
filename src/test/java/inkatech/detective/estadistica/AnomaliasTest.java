package inkatech.detective.estadistica;

import static org.assertj.core.api.Assertions.assertThat;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnomaliasTest {

    private final Anomalias anomalias = new Anomalias();

    @Test
    void subtractsTheMeanForTheSameCalendarMonth() {
        SerieTemporal serie = new SerieTemporal("temperature", "°C", List.of(
                point(2020, 1, 10.0),
                point(2020, 2, 20.0),
                point(2021, 1, 14.0),
                point(2021, 2, 24.0)));

        SerieTemporal result = anomalias.calcular(serie);

        assertThat(result.puntos())
                .extracting(PuntoTemporal::valor)
                .containsExactly(-2.0, -2.0, 2.0, 2.0);
    }

    @Test
    void excludesMissingValuesFromTheMonthlyMeanAndKeepsThemMissing() {
        SerieTemporal serie = new SerieTemporal("precipitation", "mm/month", List.of(
                point(2020, 1, 10.0),
                point(2021, 1, Double.NaN),
                point(2022, 1, 14.0)));

        SerieTemporal result = anomalias.calcular(serie);

        assertThat(result.puntos().get(0).valor()).isEqualTo(-2.0);
        assertThat(result.puntos().get(1).valor()).isNaN();
        assertThat(result.puntos().get(2).valor()).isEqualTo(2.0);
    }

    private static PuntoTemporal point(int year, int month, double value) {
        return new PuntoTemporal(LocalDate.of(year, month, 1), value);
    }
}
