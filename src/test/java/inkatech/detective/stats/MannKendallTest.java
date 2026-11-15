package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MannKendallTest {

    private static final double DELTA = 1e-9;

    @Test
    void detectaTendenciaCrecientePerfecta() {
        MannKendall.Resultado r = MannKendall.test(new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });

        assertEquals(10, r.n());
        assertEquals(45L, r.s());
        assertEquals(125.0, r.varianza(), DELTA);
        assertEquals(3.9354796403996297, r.z(), DELTA);
        assertEquals(8.303070332644974e-05, r.valorP(), DELTA);
        assertEquals(1, r.sentido());
    }

    @Test
    void corrigeLaVarianzaPorEmpates() {
        MannKendall.Resultado r = MannKendall.test(new double[] { 2, 3, 3, 4, 4, 4, 5, 6, 6, 7, 8, 8 });

        assertEquals(60L, r.s());
        assertEquals(206.0, r.varianza(), DELTA);
        assertEquals(4.110724784320544, r.z(), DELTA);
        assertEquals(3.944190958715079e-05, r.valorP(), DELTA);
    }

    @Test
    void detectaTendenciaDecreciente() {
        MannKendall.Resultado r = MannKendall.test(new double[] {
                9.0, 8.5, 8.7, 7.9, 7.4, 7.6, 6.8, 6.1, 6.5, 5.7, 5.2, 5.4 });

        assertEquals(-58L, r.s());
        assertEquals(212.66666666666666, r.varianza(), DELTA);
        assertEquals(-3.9086352435453273, r.z(), DELTA);
        assertEquals(9.281898133684614e-05, r.valorP(), DELTA);
        assertEquals(-1, r.sentido());
    }

    @Test
    void serieConstanteNoTieneTendencia() {
        MannKendall.Resultado r = MannKendall.test(new double[] { 5, 5, 5, 5, 5, 5, 5, 5 });

        assertEquals(0L, r.s());
        assertEquals(0.0, r.varianza(), DELTA);
        assertEquals(0.0, r.z(), DELTA);
        assertEquals(1.0, r.valorP(), DELTA);
        assertEquals(0, r.sentido());
    }

    @Test
    void rechazaSeriesMuyCortas() {
        assertThrows(IllegalArgumentException.class, () -> MannKendall.test(new double[] { 1, 2, 3 }));
    }

    @Test
    void rechazaValoresFaltantes() {
        assertThrows(IllegalArgumentException.class,
                () -> MannKendall.test(new double[] { 1, 2, Double.NaN, 4, 5 }));
    }
}