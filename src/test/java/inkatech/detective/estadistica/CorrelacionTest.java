package inkatech.detective.estadistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.ResultadoCorrelacion;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorrelacionTest {
    private final Correlacion correlacion = new Correlacion();

    @Test
    void correlatesTheFirstSignalWithTheSecondSignalAfterTheRequestedLag() {
        List<PuntoTemporal> firstPoints = new ArrayList<>();
        List<PuntoTemporal> secondPoints = new ArrayList<>();
        for (int index = 0; index < 36; index++) {
            LocalDate date = LocalDate.of(2010, 1, 1).plusMonths(index);
            double value = date.getYear() * 100.0 + date.getMonthValue();
            firstPoints.add(new PuntoTemporal(date, value));
            secondPoints.add(new PuntoTemporal(date.plusMonths(1), value));
        }
        SerieTemporal first = new SerieTemporal("a", "u", firstPoints);
        SerieTemporal second = new SerieTemporal("b", "u", secondPoints);

        ResultadoCorrelacion result = correlacion.calcular(first, second, 1);

        assertThat(result.coeficiente()).isEqualTo(1.0);
        assertThat(result.valorP()).isZero();
        assertThat(result.rezagoMeses()).isEqualTo(1);
        assertThat(result.numeroPares()).isEqualTo(36);
    }

    @Test
    void rejectsLagsOutsideTheSupportedRange() {
        SerieTemporal serie = serie("a", List.of(1.0, 2.0, 3.0));

        assertThatThrownBy(() -> correlacion.calcular(serie, serie, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 0 y 4");
    }

    private static SerieTemporal serie(String id, List<Double> values) {
        List<PuntoTemporal> points = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            points.add(new PuntoTemporal(LocalDate.of(2010 + index, 1, 1), values.get(index)));
        }
        return new SerieTemporal(id, "u", points);
    }
}
