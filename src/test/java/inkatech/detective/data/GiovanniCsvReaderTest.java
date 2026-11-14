package inkatech.detective.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.domain.SerieTemporal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GiovanniCsvReaderTest {

    private final GiovanniCsvReader reader = new GiovanniCsvReader();
    private final VariableConfig variable = new CatalogoVariables()
            .buscar("sea-surface-temperature")
            .orElseThrow();

    @TempDir
    Path temporaryDirectory;

    @Test
    void skipsMetadataParsesSupportedDatesAndRepresentsMissingValuesAsNaN() throws IOException {
        Path csv = writeCsv("""
                # Giovanni area-averaged time series
                # Bounding box: -90,-10,-80,0
                date,mean_value
                2020-03-01T00:00:00Z,2.55e1
                2020-01-01,24.5
                2020-02,NaN
                """);

        SerieTemporal result = reader.leer(csv, variable);

        assertThat(result.variableId()).isEqualTo("sea-surface-temperature");
        assertThat(result.unidad()).isEqualTo("°C");
        assertThat(result.puntos()).hasSize(3);
        assertThat(result.puntos().get(0).fecha()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(result.puntos().get(0).valor()).isEqualTo(24.5);
        assertThat(result.puntos().get(1).valor()).isNaN();
        assertThat(result.puntos().get(2).valor()).isEqualTo(25.5);
    }

    @Test
    void rejectsFilesWithoutObservationsAndMalformedValues() throws IOException {
        Path empty = writeCsv("# metadata only\ndate,value\n");
        assertThatThrownBy(() -> reader.leer(empty, variable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no contiene datos");

        Path malformed = writeCsv("date,value\n2020-01-01,not-a-number\n");
        assertThatThrownBy(() -> reader.leer(malformed, variable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor invalido");
    }

    private Path writeCsv(String content) throws IOException {
        Path path = temporaryDirectory.resolve("giovanni.csv");
        return Files.writeString(path, content);
    }
}
