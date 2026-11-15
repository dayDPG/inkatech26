package inkatech.detective.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RepositorioSeriesTests {

    @Test
    void cacheaLasSeriesInmutablesEntreSolicitudes() {
        VariableConfig variable = new CatalogoVariables()
                .obtenerPorId("sea-surface-temperature-total")
                .orElseThrow();
        RepositorioSeries repositorio = new RepositorioSeries(new AgregadorMensual());

        SerieTemporal primera = repositorio.obtener(variable);
        SerieTemporal segunda = repositorio.obtener(variable);

        assertSame(primera, segunda);
    }

    @Test
    void normalizaObservacionesDuplicadasDeProductosMensuales() {
        VariableConfig variable = new CatalogoVariables()
                .obtenerPorId("sea-surface-temperature-total")
                .orElseThrow();
        SerieTemporal serie = new RepositorioSeries(new AgregadorMensual()).obtener(variable);
        Set<YearMonth> meses = serie.puntos().stream()
                .map(punto -> YearMonth.from(punto.fecha()))
                .collect(Collectors.toSet());

        assertEquals(serie.puntos().size(), meses.size());
    }
}
