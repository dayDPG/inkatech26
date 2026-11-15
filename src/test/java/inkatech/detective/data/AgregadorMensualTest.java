package inkatech.detective.data;

import static org.assertj.core.api.Assertions.assertThat;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgregadorMensualTest {
    private final AgregadorMensual agregador = new AgregadorMensual();

    @Test
    void sumaLosValoresValidosDeCadaMes() {
        SerieTemporal diaria = new SerieTemporal("precipitation-norte", "mm/dia", List.of(
                new PuntoTemporal(LocalDate.of(2024, 1, 1), 1.5),
                new PuntoTemporal(LocalDate.of(2024, 1, 2), Double.NaN),
                new PuntoTemporal(LocalDate.of(2024, 1, 3), 2.5),
                new PuntoTemporal(LocalDate.of(2024, 2, 1), 3.0)));

        SerieTemporal mensual = agregador.sumar(diaria);

        assertThat(mensual.unidad()).isEqualTo("mm/mes");
        assertThat(mensual.puntos()).extracting(PuntoTemporal::valor).containsExactly(4.0, 3.0);
        assertThat(mensual.puntos()).extracting(PuntoTemporal::fecha).containsExactly(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1));
    }
}
