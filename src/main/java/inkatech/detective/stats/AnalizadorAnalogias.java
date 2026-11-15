package inkatech.detective.stats;

import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class AnalizadorAnalogias {

    private static final LocalDate CORTE_DATOS = LocalDate.of(2025, 11, 30);

    private final Correlacion correlacion;

    public AnalizadorAnalogias(Correlacion correlacion) {
        this.correlacion = Objects.requireNonNull(correlacion);
    }

    public Resultado analizar(SerieTemporal temperatura, SerieTemporal clorofila) {
        Objects.requireNonNull(temperatura, "La serie de temperatura es obligatoria");
        Objects.requireNonNull(clorofila, "La serie de clorofila es obligatoria");

        return new Resultado(
                temperatura.variableId(),
                clorofila.variableId(),
                CORTE_DATOS,
                List.of(
                        analizarEvento("2017", temperatura, clorofila,
                                LocalDate.of(2017, 1, 1), LocalDate.of(2017, 12, 31)),
                        analizarEvento("2023-2024", temperatura, clorofila,
                                LocalDate.of(2023, 1, 1), LocalDate.of(2024, 11, 30))));
    }

    private Evento analizarEvento(
            String nombre,
            SerieTemporal temperatura,
            SerieTemporal clorofila,
            LocalDate desde,
            LocalDate hasta) {
        ResultadoCorrelacion resultado = correlacion.calcular(
                temperatura,
                clorofila,
                0,
                YearMonth.from(desde),
                YearMonth.from(hasta));
        return new Evento(
                nombre,
                desde,
                hasta,
                resultado.numeroPares(),
                resultado.coeficiente(),
                resultado.valorP());
    }

    public record Resultado(
            String variableTemperatura,
            String variableClorofila,
            LocalDate corteDatosHasta,
            List<Evento> eventos) {
        public Resultado {
            eventos = List.copyOf(eventos);
        }
    }

    public record Evento(
            String nombre,
            LocalDate desde,
            LocalDate hasta,
            int numeroDePares,
            double rhoSpearman,
            double valorP) {
    }
}
