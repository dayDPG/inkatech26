package inkatech.detective.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.RepositorioSeries;
import inkatech.detective.data.SerieTemporal;
import inkatech.detective.stats.Correlacion;
import inkatech.detective.web.AnalisisDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioAnalisisTest {

    private static final String TEMPERATURA = "sea-surface-temperature-total";
    private CatalogoVariables catalogo;
    private RepositorioSeries repositorio;
    private ServicioAnalisis servicio;

    @BeforeEach
    void prepararServicio() {
        catalogo = new CatalogoVariables();
        repositorio = mock(RepositorioSeries.class);
        servicio = new ServicioAnalisis(catalogo, repositorio, new Correlacion());
        catalogo.obtenerTodas().forEach(variable ->
                when(repositorio.obtener(variable)).thenReturn(serie(variable, 72)));
    }

    @Test
    void analizaUnaSeleccionYDevuelveTendenciaEventoYRezagosDeLaCadena() {
        AnalisisDTO resultado = servicio.analizar(TEMPERATURA, "2019-01-01", "2024-12-31");

        assertEquals(72, resultado.serie().puntos().size());
        assertEquals(72, resultado.anomalias().puntos().size());
        assertNotNull(resultado.tendencia());
        assertNull(resultado.mensajeTendencia());
        assertNotNull(resultado.evento());
        assertNotNull(resultado.evento().zScore());
        assertNotNull(resultado.evento().percentil());
        assertEquals(5, resultado.evento().numeroDeMesesHistoricos());
        assertEquals(3, resultado.relaciones().size());
        resultado.relaciones().forEach(relacion -> {
            assertEquals(5, relacion.rezagos().size());
            assertTrue(relacion.rezagos().stream()
                    .allMatch(rezago -> rezago.numeroDePares() >= 0));
            assertNotNull(relacion.mejorRezago());
        });
        assertEquals(72, resultado.relaciones().getFirst().rezagos().getFirst().numeroDePares());
        assertEquals(68, resultado.relaciones().getFirst().rezagos().getLast().numeroDePares());
    }

    @Test
    void comunicaLaTendenciaNoDisponibleEnUnPeriodoCorto() {
        AnalisisDTO resultado = servicio.analizar(TEMPERATURA, "2019-01-01", "2019-06-30");

        assertNull(resultado.tendencia());
        assertNotNull(resultado.mensajeTendencia());
        assertEquals(0, resultado.evento().numeroDeMesesHistoricos());
        assertNull(resultado.evento().zScore());
    }

    @Test
    void rechazaPeriodoInvertidoYFechasConFormatoIncorrecto() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.analizar(TEMPERATURA, "2024-12-01", "2024-01-01"));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.analizar(TEMPERATURA, "ayer", "2024-01-01"));
    }

    @Test
    void rechazaUnaVariableDesconocida() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.analizar("no-existe", "2024-01-01", "2024-12-31"));
    }

    private static SerieTemporal serie(VariableConfig variable, int meses) {
        List<PuntoTemporal> puntos = new ArrayList<>();
        for (int indice = 0; indice < meses; indice++) {
            LocalDate fecha = LocalDate.of(2019, 1, 1).plusMonths(indice);
            double estacionalidad = Math.sin(fecha.getMonthValue() * 0.71);
            double ruido = Math.sin(indice * 1.37) * 0.2;
            double tendencia = indice * 0.03;
            double signo = variable.grupo().equals("chlorophyll-a") ? -1.0 : 1.0;
            puntos.add(new PuntoTemporal(fecha, estacionalidad + ruido + signo * tendencia));
        }
        return new SerieTemporal(variable.id(), variable.unidad(), puntos);
    }
}
