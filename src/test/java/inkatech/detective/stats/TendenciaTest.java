package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TendenciaTest {

    @Test
    void esSignificativaSoloSiElValorPEsMenorQueElNivel() {
        assertTrue(new Tendencia(1.0, 0.049, 0.5, 1.5, 30).esSignificativa());
        assertFalse(new Tendencia(1.0, 0.05, 0.5, 1.5, 30).esSignificativa());
        assertFalse(new Tendencia(1.0, 0.4, 0.5, 1.5, 30).esSignificativa());
    }

    @Test
    void rechazaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Tendencia(1.0, 1.2, 0.5, 1.5, 30));
        assertThrows(IllegalArgumentException.class, () -> new Tendencia(1.0, -0.1, 0.5, 1.5, 30));
        assertThrows(IllegalArgumentException.class, () -> new Tendencia(1.0, 0.5, 2.0, 1.5, 30));
        assertThrows(IllegalArgumentException.class, () -> new Tendencia(Double.NaN, 0.5, 0.5, 1.5, 30));
        assertThrows(IllegalArgumentException.class, () -> new Tendencia(1.0, 0.5, 0.5, 1.5, 0));
    }
}