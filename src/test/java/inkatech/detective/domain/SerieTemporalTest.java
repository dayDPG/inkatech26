package inkatech.detective.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SerieTemporalTest {

    @Test
    void createsAnImmutableChronologicalSeriesAndPreservesMissingValues() {
        List<PuntoTemporal> source = new ArrayList<>(List.of(
                new PuntoTemporal(LocalDate.of(2025, 1, 1), 24.5),
                new PuntoTemporal(LocalDate.of(2025, 2, 1), Double.NaN)));

        SerieTemporal serie = new SerieTemporal("sea-surface-temperature", "°C", source);
        source.clear();

        assertThat(serie.puntos()).hasSize(2);
        assertThat(serie.puntos().get(1).valor()).isNaN();
        assertThatThrownBy(() -> serie.puntos().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsRepeatedOrUnorderedDates() {
        LocalDate date = LocalDate.of(2025, 1, 1);

        assertThatThrownBy(() -> new SerieTemporal(
                "precipitation",
                "mm/month",
                List.of(new PuntoTemporal(date, 10.0), new PuntoTemporal(date, 12.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ordenados por fecha");
    }

    @Test
    void rejectsBlankMetadataAndInfiniteValues() {
        assertThatThrownBy(() -> new SerieTemporal(" ", "°C", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");

        assertThatThrownBy(() -> new PuntoTemporal(LocalDate.of(2025, 1, 1), Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("infinito");
    }
}
