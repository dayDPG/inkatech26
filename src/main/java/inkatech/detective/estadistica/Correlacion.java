package inkatech.detective.estadistica;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.ResultadoCorrelacion;
import inkatech.detective.domain.SerieTemporal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.springframework.stereotype.Component;

@Component
public final class Correlacion {
    private static final int MINIMUM_PAIRS = 3;
    private static final int MAXIMUM_LAG = 4;

    private final Anomalias anomalias = new Anomalias();

    public ResultadoCorrelacion calcular(SerieTemporal primera, SerieTemporal segunda, int rezagoMeses) {
        Objects.requireNonNull(primera, "La primera serie es obligatoria");
        Objects.requireNonNull(segunda, "La segunda serie es obligatoria");
        if (rezagoMeses < 0 || rezagoMeses > MAXIMUM_LAG) {
            throw new IllegalArgumentException("El rezago debe estar entre 0 y 4 meses");
        }

        SerieTemporal primeraMensual = anomalias.calcular(normalizarMensual(primera));
        SerieTemporal segundaMensual = anomalias.calcular(normalizarMensual(segunda));
        Map<YearMonth, Double> valoresSegunda = indexar(segundaMensual);
        List<Double> valoresPrimera = new ArrayList<>();
        List<Double> valoresRezago = new ArrayList<>();

        for (PuntoTemporal punto : primeraMensual.puntos()) {
            if (!Double.isFinite(punto.valor())) continue;
            Double valorSegunda = valoresSegunda.get(YearMonth.from(punto.fecha()).plusMonths(rezagoMeses));
            if (valorSegunda != null && Double.isFinite(valorSegunda)) {
                valoresPrimera.add(punto.valor());
                valoresRezago.add(valorSegunda);
            }
        }

        if (valoresPrimera.size() < MINIMUM_PAIRS) {
            throw new IllegalArgumentException("La correlacion requiere al menos tres pares validos");
        }

        double[] x = valoresPrimera.stream().mapToDouble(Double::doubleValue).toArray();
        double[] y = valoresRezago.stream().mapToDouble(Double::doubleValue).toArray();
        double coefficient = new SpearmansCorrelation().correlation(x, y);
        if (!Double.isFinite(coefficient)) {
            throw new IllegalArgumentException("La correlacion no se puede calcular con una serie constante");
        }

        return new ResultadoCorrelacion(
                coefficient,
                calcularValorP(coefficient, x.length),
                rezagoMeses,
                x.length);
    }

    private static SerieTemporal normalizarMensual(SerieTemporal serie) {
        Map<YearMonth, Acumulador> meses = new LinkedHashMap<>();
        for (PuntoTemporal punto : serie.puntos()) {
            if (Double.isFinite(punto.valor())) {
                meses.computeIfAbsent(YearMonth.from(punto.fecha()), ignored -> new Acumulador())
                        .agregar(punto.valor());
            }
        }
        List<PuntoTemporal> puntos = meses.entrySet().stream()
                .map(entry -> new PuntoTemporal(entry.getKey().atDay(1), entry.getValue().promedio()))
                .toList();
        return new SerieTemporal(serie.variableId(), serie.unidad(), puntos);
    }

    private static Map<YearMonth, Double> indexar(SerieTemporal serie) {
        Map<YearMonth, Double> result = new LinkedHashMap<>();
        serie.puntos().forEach(punto -> result.put(YearMonth.from(punto.fecha()), punto.valor()));
        return result;
    }

    private static double calcularValorP(double coefficient, int observations) {
        if (Math.abs(coefficient) >= 1.0) return 0.0;
        double statistic = coefficient * Math.sqrt((observations - 2.0) / (1.0 - coefficient * coefficient));
        TDistribution distribution = new TDistribution(observations - 2.0);
        return 2.0 * (1.0 - distribution.cumulativeProbability(Math.abs(statistic)));
    }

    private static final class Acumulador {
        private double suma;
        private int cantidad;

        void agregar(double valor) {
            suma += valor;
            cantidad++;
        }

        double promedio() {
            return suma / cantidad;
        }
    }
}
