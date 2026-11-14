package inkatech.detective.estadistica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import inkatech.detective.domain.Tendencia;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AnalizadorTendenciaTest {

    private final AnalizadorTendencia analizador = new AnalizadorTendencia();

    @Test
    void combinesRobustSlopeSignificanceAndRegressionInterval() {
        SerieTemporal serie = annualSeries(10.0, 10.5, 11.0, 11.5, 12.0);

        Tendencia result = analizador.calcular(serie);

        assertThat(result.pendientePorDecada()).isCloseTo(5.0, offset(1.0e-10));
        assertThat(result.valorP()).isCloseTo(0.0274863361, offset(1.0e-9));
        assertThat(result.intervaloConfianzaInferior()).isCloseTo(5.0, offset(1.0e-10));
        assertThat(result.intervaloConfianzaSuperior()).isCloseTo(5.0, offset(1.0e-10));
        assertThat(result.isSignificativa()).isTrue();
    }

    @Test
    void reportsAFlatSeriesAsNotSignificant() {
        Tendencia result = analizador.calcular(annualSeries(7, 7, 7, 7));

        assertThat(result.pendientePorDecada()).isZero();
        assertThat(result.valorP()).isEqualTo(1.0);
        assertThat(result.intervaloConfianzaInferior()).isZero();
        assertThat(result.intervaloConfianzaSuperior()).isZero();
        assertThat(result.isSignificativa()).isFalse();
    }

    @Test
    void requiresThreeValidObservations() {
        assertThatThrownBy(() -> analizador.calcular(annualSeries(1, Double.NaN, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tres observaciones validas");
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
