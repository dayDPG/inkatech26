package inkatech.detective.estadistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MannKendallTest {

    private final MannKendall mannKendall = new MannKendall();

    @Test
    void calculatesAnIncreasingTrendWithoutTies() {
        ResultadoMannKendall result = mannKendall.calcular(series(1, 2, 3, 4, 5));

        assertThat(result.s()).isEqualTo(10);
        assertThat(result.varianza()).isCloseTo(16.6666666667, within(1.0e-10));
        assertThat(result.z()).isCloseTo(2.2045407685, within(1.0e-10));
        assertThat(result.valorP()).isCloseTo(0.0274863361, within(1.0e-9));
    }

    @Test
    void correctsTheVarianceForTies() {
        ResultadoMannKendall result = mannKendall.calcular(series(1, 2, 2, 3));

        assertThat(result.s()).isEqualTo(5);
        assertThat(result.varianza()).isCloseTo(7.6666666667, within(1.0e-10));
        assertThat(result.z()).isCloseTo(1.4446302370, within(1.0e-10));
        assertThat(result.valorP()).isCloseTo(0.1485617749, within(1.0e-9));
    }

    @Test
    void returnsNoTrendWhenAllValuesAreTied() {
        ResultadoMannKendall result = mannKendall.calcular(series(7, 7, 7, 7));

        assertThat(result.s()).isZero();
        assertThat(result.varianza()).isZero();
        assertThat(result.z()).isZero();
        assertThat(result.valorP()).isEqualTo(1.0);
    }

    @Test
    void ignoresMissingValuesAndRequiresTwoValidObservations() {
        ResultadoMannKendall result = mannKendall.calcular(series(1, Double.NaN, 3));
        assertThat(result.s()).isEqualTo(1);

        assertThatThrownBy(() -> mannKendall.calcular(series(Double.NaN, 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dos observaciones validas");
    }

    private static SerieTemporal series(double... values) {
        List<PuntoTemporal> points = IntStream.range(0, values.length)
                .mapToObj(index -> new PuntoTemporal(
                        LocalDate.of(2020, 1, 1).plusMonths(index),
                        values[index]))
                .toList();
        return new SerieTemporal("test", "unit", points);
    }

    private static org.assertj.core.data.Offset<Double> within(double tolerance) {
        return org.assertj.core.data.Offset.offset(tolerance);
    }
}
