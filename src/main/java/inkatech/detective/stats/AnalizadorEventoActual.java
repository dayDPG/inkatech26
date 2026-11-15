package inkatech.detective.stats;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class AnalizadorEventoActual {

    private static final int ANIO_BASE_DESDE = 2002;
    private static final int ANIO_BASE_HASTA = 2025;
    private static final int ANIO_EVENTO = 2026;
    private static final int ULTIMO_MES = 8;
    private static final int MESES_MINIMOS = 6;
    private static final int ANIOS_BASE_MINIMOS = 10;

    public Resultado analizar(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");

        Map<Integer, List<Double>> valoresPorAnio = new HashMap<>();
        for (PuntoTemporal punto : serie.puntos()) {
            YearMonth mes = YearMonth.from(punto.fecha());
            if (mes.getMonthValue() > ULTIMO_MES || punto.esFaltante() || punto.parcial()) {
                continue;
            }
            if ((mes.getYear() >= ANIO_BASE_DESDE && mes.getYear() <= ANIO_BASE_HASTA)
                    || mes.getYear() == ANIO_EVENTO) {
                valoresPorAnio.computeIfAbsent(mes.getYear(), ignorado -> new ArrayList<>())
                        .add(punto.valor());
            }
        }

        List<Double> base = new ArrayList<>();
        int primerAnioBase = -1;
        int ultimoAnioBase = -1;
        for (int anio = ANIO_BASE_DESDE; anio <= ANIO_BASE_HASTA; anio++) {
            List<Double> valores = valoresPorAnio.getOrDefault(anio, List.of());
            if (valores.size() >= MESES_MINIMOS) {
                base.add(promedio(valores));
                if (primerAnioBase == -1) {
                    primerAnioBase = anio;
                }
                ultimoAnioBase = anio;
            }
        }
        if (base.size() < ANIOS_BASE_MINIMOS) {
            throw new IllegalArgumentException(
                    "Se requieren al menos " + ANIOS_BASE_MINIMOS + " años válidos para construir la base");
        }

        List<Double> actual = valoresPorAnio.getOrDefault(ANIO_EVENTO, List.of());
        if (actual.size() < MESES_MINIMOS) {
            throw new IllegalArgumentException(
                    "El evento 2026 necesita al menos " + MESES_MINIMOS + " meses completos entre enero y agosto");
        }

        double promedioActual = promedio(actual);
        double mediaBase = promedio(base);
        double desviacionBase = desviacionMuestral(base, mediaBase);
        if (desviacionBase == 0.0) {
            throw new IllegalArgumentException("La serie no presenta variación suficiente en el período base");
        }

        double menores = base.stream().filter(valor -> valor < promedioActual).count();
        double iguales = base.stream().filter(valor -> valor == promedioActual).count();
        double percentil = 100.0 * (menores + iguales / 2.0) / base.size();
        double desviacionEvento = Math.abs(promedioActual - mediaBase);
        long extremosIgualOMayores = base.stream()
                .filter(valor -> Math.abs(valor - mediaBase) >= desviacionEvento)
                .count();
        double probabilidadEmpirica = (extremosIgualOMayores + 1.0) / (base.size() + 1.0);

        return new Resultado(
                primerAnioBase,
                ultimoAnioBase,
                base.size(),
                actual.size(),
                promedioActual,
                mediaBase,
                desviacionBase,
                (promedioActual - mediaBase) / desviacionBase,
                percentil,
                1.0 / probabilidadEmpirica);
    }

    private static double promedio(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static double desviacionMuestral(List<Double> valores, double media) {
        double sumaCuadrados = valores.stream()
                .mapToDouble(valor -> Math.pow(valor - media, 2.0))
                .sum();
        return Math.sqrt(sumaCuadrados / (valores.size() - 1.0));
    }

    public record Resultado(
            int periodoBaseDesde,
            int periodoBaseHasta,
            int numeroDeAniosBase,
            int mesesValidos2026,
            double promedioEneAgo2026,
            double mediaBase,
            double desviacionBase,
            double zScore,
            double percentil,
            double periodoRetornoAnios) {
    }
}
