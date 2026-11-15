package inkatech.detective.stats;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class AnalizadorValidacionLluvia {

    private static final YearMonth DESDE = YearMonth.of(2002, 8);
    private static final YearMonth HASTA = YearMonth.of(2025, 9);

    private final Correlacion correlacion;

    public AnalizadorValidacionLluvia(Correlacion correlacion) {
        this.correlacion = Objects.requireNonNull(correlacion);
    }

    public Resultado analizar(SerieTemporal late, SerieTemporal finalMensual) {
        Objects.requireNonNull(late, "La serie Late es obligatoria");
        Objects.requireNonNull(finalMensual, "La serie Final es obligatoria");

        SerieTemporal lateComun = late.filtrarPorRango(DESDE.atDay(1), HASTA.atEndOfMonth());
        SerieTemporal finalComun = finalMensual.filtrarPorRango(DESDE.atDay(1), HASTA.atEndOfMonth());
        ResultadoCorrelacion resultadoCorrelacion = correlacion.calcular(lateComun, finalComun, 0);

        Map<YearMonth, PuntoTemporal> puntosFinal = new HashMap<>();
        for (PuntoTemporal punto : finalComun.puntos()) {
            puntosFinal.put(YearMonth.from(punto.fecha()), punto);
        }

        List<Double> diferencias = new ArrayList<>();
        YearMonth primerMes = null;
        YearMonth ultimoMes = null;
        for (PuntoTemporal puntoLate : lateComun.puntos()) {
            YearMonth mes = YearMonth.from(puntoLate.fecha());
            PuntoTemporal puntoFinal = puntosFinal.get(mes);
            if (!esValido(puntoLate) || !esValido(puntoFinal)) {
                continue;
            }
            diferencias.add(puntoLate.valor() - puntoFinal.valor());
            if (primerMes == null) {
                primerMes = mes;
            }
            ultimoMes = mes;
        }

        double sesgoMedio = diferencias.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        return new Resultado(
                primerMes.atDay(1),
                ultimoMes.atEndOfMonth(),
                diferencias.size(),
                "Spearman",
                resultadoCorrelacion.coeficiente(),
                resultadoCorrelacion.valorP(),
                sesgoMedio,
                "mm/mes");
    }

    private static boolean esValido(PuntoTemporal punto) {
        return punto != null && !punto.esFaltante() && !punto.parcial();
    }

    public record Resultado(
            LocalDate desde,
            LocalDate hasta,
            int numeroDeMeses,
            String metodoCorrelacion,
            double correlacion,
            double valorP,
            double sesgoMedioLateMenosFinal,
            String unidad) {
    }
}
