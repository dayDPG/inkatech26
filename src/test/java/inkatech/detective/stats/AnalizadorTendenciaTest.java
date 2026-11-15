package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;


class AnalizadorTendenciaTest {

    private static final double DELTA = 1e-9;

    private static List<PuntoTemporal> puntosMensuales(int meses) {
        double[] patron = { 0.3, -0.1, 0.2, -0.4, 0.1, 0.0 };
        List<PuntoTemporal> puntos = new ArrayList<>();
        for (int i = 0; i < meses; i++) {
            puntos.add(new PuntoTemporal(
                    LocalDate.of(2020, 1, 1).plusMonths(i), 0.01 * i + patron[i % 6]));
        }
        return puntos;
    }

    @Test
    void calculaPendientePorDecadaIntervaloYValorP() {
        SerieTemporal serie = new SerieTemporal("prueba", "u", puntosMensuales(36));

        Optional<Tendencia> resultado = AnalizadorTendencia.analizar(serie);

        assertTrue(resultado.isPresent());
        Tendencia t = resultado.get();
        assertEquals(1.2, t.pendientePorDecada(), DELTA);
        assertEquals(0.0023076923076923053 * 120, t.intervaloConfianzaInferior(), DELTA);
        assertEquals(0.014545454545454545 * 120, t.intervaloConfianzaSuperior(), DELTA);
        assertEquals(0.0246114402001047, t.valorP(), DELTA);
        assertEquals(36, t.numeroDePuntos());
        assertTrue(t.esSignificativa());
    }

    @Test
    void ignoraFaltantesYMesesParciales() {
        List<PuntoTemporal> puntos = puntosMensuales(36);
        puntos.add(new PuntoTemporal(LocalDate.of(2020, 1, 1).plusMonths(36), Double.NaN));
        puntos.add(new PuntoTemporal(LocalDate.of(2020, 1, 1).plusMonths(37), 100.0, true));
        SerieTemporal serie = new SerieTemporal("prueba", "u", puntos);

        Tendencia t = AnalizadorTendencia.analizar(serie).orElseThrow();

        assertEquals(1.2, t.pendientePorDecada(), DELTA);
        assertEquals(36, t.numeroDePuntos());
    }

    @Test
    void devuelveVacioSiHayPocosMeses() {
        SerieTemporal serie = new SerieTemporal("prueba", "u", puntosMensuales(23));

        assertTrue(AnalizadorTendencia.analizar(serie).isEmpty());
    }

    @Test
    void rechazaSeriesQueNoSonMensuales() {
        List<PuntoTemporal> diarios = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            diarios.add(new PuntoTemporal(LocalDate.of(2020, 1, 1).plusDays(i), i));
        }
        SerieTemporal serie = new SerieTemporal("prueba", "u", diarios);

        assertThrows(IllegalArgumentException.class, () -> AnalizadorTendencia.analizar(serie));
    }
}