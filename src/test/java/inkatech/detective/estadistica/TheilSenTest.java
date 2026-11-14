package inkatech.detective.estadistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class TheilSenTest {

    private final TheilSen theilSen = new TheilSen();

    @Test
    void calculatesTheMedianPairSlopeAndResistsAnOutlier() {
        SerieTemporal serie = annualSeries(1, 3, 105, 7, 9);

        ResultadoTheilSen result = theilSen.calcular(serie);

        assertThat(result.pendienteAnual()).isCloseTo(2.0, offset(1.0e-12));
        assertThat(result.pendientePorDecada()).isCloseTo(20.0, offset(1.0e-11));
        assertThat(result.cantidadPendientes()).isEqualTo(10);
    }

    @Test
    void ignoresMissingValuesAndRequiresTwoValidObservations() {
        ResultadoTheilSen result = theilSen.calcular(annualSeries(1, Double.NaN, 5));
        assertThat(result.pendienteAnual()).isCloseTo(2.0, offset(1.0e-12));

        assertThatThrownBy(() -> theilSen.calcular(annualSeries(Double.NaN, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dos observaciones validas");
    }

    private static SerieTemporal annualSeries(double... values) {
        List<PuntoTemporal> points = IntStream.range(0, values.length)
                .mapToObj(index -> new PuntoTemporal(
                        LocalDate.of(2020 + index, 1, 1),
                        values[index]))
                .toList();
        return new SerieTemporal("test", "unit", points);
    }
}
