package inkatech.detective.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TendenciaTest {

    @Test
    void reportsSignificanceAtTheAgreedThreshold() {
        Tendencia significant = new Tendencia(0.8, 0.05, 0.2, 1.4);
        Tendencia notSignificant = new Tendencia(0.3, 0.051, -0.1, 0.7);

        assertThat(significant.isSignificativa()).isTrue();
        assertThat(notSignificant.isSignificativa()).isFalse();
    }

    @Test
    void rejectsInvalidProbabilityAndConfidenceInterval() {
        assertThatThrownBy(() -> new Tendencia(0.8, 1.01, 0.2, 1.4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 0 y 1");

        assertThatThrownBy(() -> new Tendencia(0.8, 0.02, 1.4, 0.2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limite inferior");
    }
}
