package inkatech.detective.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AgregadorMensualTests {

    private final AgregadorMensual agregador = new AgregadorMensual();

    @Test
    void sumaLosDiasYMarcaCompletoUnMesConTodosSusDias() {
        List<PuntoTemporal> puntos = IntStream.rangeClosed(1, 31)
                .mapToObj(dia -> new PuntoTemporal(LocalDate.of(2026, 1, dia), 1.0))
                .toList();

        SerieTemporal resultado = agregador.agregarDiaria(
                new SerieTemporal("lluvia", "mm/dia", puntos));

        assertEquals("mm/mes", resultado.unidad());
        assertEquals(1, resultado.puntos().size());
        assertEquals(31.0, resultado.puntos().getFirst().valor());
        assertFalse(resultado.puntos().getFirst().parcial());
    }

    @Test
    void marcaNoviembreComoParcialCuandoSoloHayDatosHastaElDiaTrece() {
        List<PuntoTemporal> puntos = IntStream.rangeClosed(1, 13)
                .mapToObj(dia -> new PuntoTemporal(LocalDate.of(2026, 11, dia), 1.0))
                .toList();

        SerieTemporal resultado = agregador.agregarDiaria(
                new SerieTemporal("lluvia", "mm/día", puntos));

        PuntoTemporal noviembre = resultado.puntos().getFirst();
        assertEquals(LocalDate.of(2026, 11, 1), noviembre.fecha());
        assertEquals(13.0, noviembre.valor());
        assertTrue(noviembre.parcial());
    }

    @Test
    void convierteLaTasaMensualUsandoLaDuracionRealDelMes() {
        SerieTemporal serie = new SerieTemporal(
                "lluvia-final",
                "mm/hr",
                List.of(new PuntoTemporal(LocalDate.of(2024, 2, 1), 2.0)));

        SerieTemporal resultado = agregador.normalizarPrecipitacionMensual(serie);

        assertEquals("mm/mes", resultado.unidad());
        assertEquals(2.0 * 29 * 24, resultado.puntos().getFirst().valor());
    }

    @Test
    void noConvierteDeNuevoUnaSerieYaExpresadaEnMilimetrosPorMes() {
        SerieTemporal serie = new SerieTemporal(
                "lluvia",
                "mm/mes",
                List.of(new PuntoTemporal(LocalDate.of(2026, 11, 1), 12.5, true)));

        SerieTemporal resultado = agregador.normalizarPrecipitacionMensual(serie);

        assertEquals(serie, resultado);
    }

    @Test
    void promediaObservacionesRepetidasEnElMismoMes() {
        SerieTemporal serie = new SerieTemporal(
                "temperatura",
                "°C",
                List.of(
                        new PuntoTemporal(LocalDate.of(2020, 9, 1), 18.0),
                        new PuntoTemporal(LocalDate.of(2020, 9, 30), 20.0)));

        SerieTemporal resultado = AgregadorMensual.promediarPorMes(serie);

        assertEquals(1, resultado.puntos().size());
        assertEquals(LocalDate.of(2020, 9, 1), resultado.puntos().getFirst().fecha());
        assertEquals(19.0, resultado.puntos().getFirst().valor());
    }

    @Test
    void conservaComoFaltanteElMesSinValoresValidos() {
        SerieTemporal serie = new SerieTemporal(
                "temperatura",
                "°C",
                List.of(
                        new PuntoTemporal(LocalDate.of(2020, 9, 1), 18.0),
                        new PuntoTemporal(LocalDate.of(2020, 10, 1), Double.NaN)));

        SerieTemporal resultado = AgregadorMensual.promediarPorMes(serie);

        assertEquals(2, resultado.puntos().size());
        assertTrue(resultado.puntos().get(1).esFaltante());
    }

    @Test
    void dejaParcialElMesConUnDiaFaltanteYUnaObservacionNaN() {
        List<PuntoTemporal> puntos = IntStream.rangeClosed(1, 31)
                .filter(dia -> dia != 20)
                .mapToObj(dia -> new PuntoTemporal(
                        LocalDate.of(2024, 1, dia),
                        dia == 10 ? Double.NaN : 1.0))
                .toList();

        PuntoTemporal enero = agregador.agregarDiaria(
                new SerieTemporal("lluvia", "mm/dia", puntos)).puntos().getFirst();

        assertEquals(29.0, enero.valor());
        assertTrue(enero.parcial());
    }

    @Test
    void conservaComoNaNUnMesCuyosDiasSonTodosFaltantes() {
        List<PuntoTemporal> puntos = IntStream.rangeClosed(1, 29)
                .mapToObj(dia -> new PuntoTemporal(
                        LocalDate.of(2024, 2, dia), Double.NaN))
                .toList();

        PuntoTemporal febrero = agregador.agregarDiaria(
                new SerieTemporal("lluvia", "mm/dia", puntos)).puntos().getFirst();

        assertTrue(febrero.esFaltante());
        assertTrue(febrero.parcial());
    }
}
