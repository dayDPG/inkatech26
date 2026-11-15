package inkatech.detective.stats;

import inkatech.detective.data.AgregadorMensual;
import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.special.Beta;
import org.springframework.stereotype.Component;

@Component
public final class Correlacion {
    private static final int MINIMUM_PAIRS = 3;
    private static final int MAXIMUM_LAG = 4;

    private final Anomalias anomalias = new Anomalias();

    public ResultadoCorrelacion calcular(SerieTemporal primera, SerieTemporal segunda, int rezagoMeses) {
        return calcular(primera, segunda, rezagoMeses, null, null);
    }

    public ResultadoCorrelacion calcular(
            SerieTemporal primera,
            SerieTemporal segunda,
            int rezagoMeses,
            YearMonth desde,
            YearMonth hasta) {
        validarEntradas(primera, segunda, rezagoMeses, desde, hasta);
        Pares pares = emparejar(primera, segunda, rezagoMeses, desde, hasta);
        if (pares.primera().length < MINIMUM_PAIRS) {
            throw new IllegalArgumentException("La correlacion requiere al menos tres pares validos");
        }

        double coefficient = new SpearmansCorrelation().correlation(pares.primera(), pares.segunda());
        if (!Double.isFinite(coefficient)) {
            throw new IllegalArgumentException("La correlacion no se puede calcular con una serie constante");
        }

        return new ResultadoCorrelacion(
                coefficient,
                calcularValorP(coefficient, pares.primera().length),
                rezagoMeses,
                pares.primera().length);
    }

    public int contarPares(
            SerieTemporal primera,
            SerieTemporal segunda,
            int rezagoMeses,
            YearMonth desde,
            YearMonth hasta) {
        validarEntradas(primera, segunda, rezagoMeses, desde, hasta);
        return emparejar(primera, segunda, rezagoMeses, desde, hasta).primera().length;
    }

    private static void validarEntradas(
            SerieTemporal primera,
            SerieTemporal segunda,
            int rezagoMeses,
            YearMonth desde,
            YearMonth hasta) {
        Objects.requireNonNull(primera, "La primera serie es obligatoria");
        Objects.requireNonNull(segunda, "La segunda serie es obligatoria");
        if (rezagoMeses < 0 || rezagoMeses > MAXIMUM_LAG) {
            throw new IllegalArgumentException("El rezago debe estar entre 0 y 4 meses");
        }
        if ((desde == null) != (hasta == null) || (desde != null && desde.isAfter(hasta))) {
            throw new IllegalArgumentException("El rango de fechas de correlación no es válido");
        }
    }

    private Pares emparejar(
            SerieTemporal primera,
            SerieTemporal segunda,
            int rezagoMeses,
            YearMonth desde,
            YearMonth hasta) {
        SerieTemporal primeraMensual = anomalias.calcular(
                AgregadorMensual.promediarPorMes(primera.sinParciales()));
        SerieTemporal segundaMensual = anomalias.calcular(
                AgregadorMensual.promediarPorMes(segunda.sinParciales()));
        Map<YearMonth, Double> valoresSegunda = indexar(segundaMensual);
        List<Double> valoresPrimera = new ArrayList<>();
        List<Double> valoresRezago = new ArrayList<>();

        for (PuntoTemporal punto : primeraMensual.puntos()) {
            if (!Double.isFinite(punto.valor())) continue;
            YearMonth mesPrimera = YearMonth.from(punto.fecha());
            YearMonth mesSegunda = mesPrimera.plusMonths(rezagoMeses);
            if (desde != null
                    && (mesPrimera.isBefore(desde) || mesPrimera.isAfter(hasta)
                            || mesSegunda.isBefore(desde) || mesSegunda.isAfter(hasta))) {
                continue;
            }
            Double valorSegunda = valoresSegunda.get(mesSegunda);
            if (valorSegunda != null && Double.isFinite(valorSegunda)) {
                valoresPrimera.add(punto.valor());
                valoresRezago.add(valorSegunda);
            }
        }

        double[] x = valoresPrimera.stream().mapToDouble(Double::doubleValue).toArray();
        double[] y = valoresRezago.stream().mapToDouble(Double::doubleValue).toArray();
        return new Pares(x, y);
    }

    private record Pares(double[] primera, double[] segunda) {}

    private static Map<YearMonth, Double> indexar(SerieTemporal serie) {
        Map<YearMonth, Double> result = new LinkedHashMap<>();
        serie.puntos().forEach(punto -> result.put(YearMonth.from(punto.fecha()), punto.valor()));
        return result;
    }

    private static double calcularValorP(double coefficient, int observations) {
        if (Math.abs(coefficient) >= 1.0) return 0.0;
        double gradosLibertad = observations - 2.0;
        double estadistico = coefficient * Math.sqrt(
                gradosLibertad / (1.0 - coefficient * coefficient));
        double x = gradosLibertad / (gradosLibertad + estadistico * estadistico);
        return Beta.regularizedBeta(x, gradosLibertad / 2.0, 0.5);
    }
}