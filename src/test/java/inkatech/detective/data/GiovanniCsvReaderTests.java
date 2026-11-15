package inkatech.detective.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GiovanniCsvReaderTests {

    @TempDir
    Path directorioTemporal;

    @Test
    void interpretaMarcadoresDeGiovanniComoValoresFaltantes() throws IOException {
        Path csv = directorioTemporal.resolve("serie.csv");
        Files.writeString(csv, """
                Metadata
                time,value
                2020-01-01,1.5
                2020-01-02,NaN
                2020-01-03,-9999.9
                2020-01-04,-32767
                2020-01-05,
                """);
        VariableConfig variable = new CatalogoVariables()
                .obtenerPorId("sea-surface-temperature-total")
                .orElseThrow();

        SerieTemporal serie = GiovanniCsvReader.leer(csv, variable);

        assertEquals(5, serie.puntos().size());
        assertEquals(1.5, serie.puntos().getFirst().valor());
        assertTrue(serie.puntos().subList(1, 5).stream().allMatch(PuntoTemporal::esFaltante));
    }

    @Test
    void rechazaTextoNoNumericoEnLaColumnaDeValores() throws IOException {
        Path csv = directorioTemporal.resolve("serie-invalida.csv");
        Files.writeString(csv, """
                time,value
                2020-01-01,no-es-un-numero
                """);
        VariableConfig variable = new CatalogoVariables()
                .obtenerPorId("sea-surface-temperature-total")
                .orElseThrow();

        assertThrows(IllegalArgumentException.class, () -> GiovanniCsvReader.leer(csv, variable));
    }

    @Test
    void leeElCsvDelClasspathParaEjecucionesEmpaquetadas() throws IOException {
        VariableConfig variable = new CatalogoVariables()
                .obtenerPorId("sea-surface-temperature-total")
                .orElseThrow();
        InputStream entrada = getClass().getClassLoader()
                .getResourceAsStream("inkatech/detective/csv/temp_mar.csv");
        assertNotNull(entrada);

        try (entrada) {
            SerieTemporal serie = GiovanniCsvReader.leer(entrada, variable);
            assertTrue(serie.puntos().size() > 0);
        }
    }
}
