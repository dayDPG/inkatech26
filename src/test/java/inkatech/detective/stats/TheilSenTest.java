package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TheilSenTest {

    private static final double DELTA = 1e-12;

    private static double[] indices(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
        }
        return x;
    }

    @Test
    void lineaPerfectaTienePendienteExactaEIntervaloDegenerado() {
        double[] x = indices(10);
        double[] y = new double[10];
        for (int i = 0; i < 10; i++) {
            y[i] = 2.0 * i + 1.0;
        }

        TheilSen.Resultado r = TheilSen.estimar(x, y);

        assertEquals(2.0, r.pendiente(), DELTA);
        assertEquals(2.0, r.limiteInferior(), DELTA);
        assertEquals(2.0, r.limiteSuperior(), DELTA);
        assertEquals(10, r.n());
    }

    @Test
    void funcionaConHuecosEnX() {
        double[] x = { 0, 1, 2, 4, 5, 7, 8, 9, 11, 12, 14, 15, 17, 18, 19, 21, 22, 23 };
        double[] y = { 1.0, 1.4, 1.9, 3.1, 3.4, 4.6, 5.2, 5.5, 6.9, 7.1,
                8.6, 8.9, 10.4, 10.1, 11.2, 12.9, 12.7, 13.8 };

        TheilSen.Resultado r = TheilSen.estimar(x, y);

        assertEquals(0.5470588235294117, r.pendiente(), DELTA);
        assertEquals(0.5272727272727273, r.limiteInferior(), DELTA);
        assertEquals(0.5666666666666668, r.limiteSuperior(), DELTA);
    }

    @Test
    void corrigePorEmpates() {
        double[] y = { 2, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 8 };

        TheilSen.Resultado r = TheilSen.estimar(indices(12), y);

        assertEquals(0.5, r.pendiente(), DELTA);
        assertEquals(0.5, r.limiteInferior(), DELTA);
        assertEquals(0.6666666666666666, r.limiteSuperior(), DELTA);
    }

    @Test
    void detectaPendienteNegativa() {
        double[] y = { 9.0, 8.5, 8.7, 7.9, 7.4, 7.6, 6.8, 6.1, 6.5, 5.7, 5.2, 5.4 };

        TheilSen.Resultado r = TheilSen.estimar(indices(12), y);

        assertEquals(-0.36666666666666653, r.pendiente(), DELTA);
        assertEquals(-0.4142857142857143, r.limiteInferior(), DELTA);
        assertEquals(-0.2999999999999998, r.limiteSuperior(), DELTA);
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> TheilSen.estimar(new double[] { 0, 1, 2 }, new double[] { 1, 2, 3 }));
        assertThrows(IllegalArgumentException.class,
                () -> TheilSen.estimar(indices(5), new double[] { 1, 2, 3 }));
        assertThrows(IllegalArgumentException.class,
                () -> TheilSen.estimar(indices(5), new double[] { 1, 2, Double.NaN, 4, 5 }));
        assertThrows(IllegalArgumentException.class,
                () -> TheilSen.estimar(new double[] { 1, 1, 1, 1 }, new double[] { 1, 2, 3, 4 }));
    }
}