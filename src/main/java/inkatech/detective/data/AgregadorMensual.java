package inkatech.detective.data;

import java.text.Normalizer;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AgregadorMensual {

    public SerieTemporal normalizarPrecipitacionMensual(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie de precipitacion es obligatoria");

        return switch (normalizarUnidad(serie.unidad())) {
            case "mm/dia", "mm/day" -> agregarDiaria(serie);
            case "mm/hr", "mm/h", "mm/hour" -> convertirMmHoraAMmMes(serie);
            case "mm/mes", "mm/month" -> serie;
            default -> throw new IllegalArgumentException(
                    "Unidad de precipitacion no compatible: " + serie.unidad());
        };
    }

    public SerieTemporal agregarDiaria(SerieTemporal serieDiaria) {
        Objects.requireNonNull(serieDiaria, "La serie diaria es obligatoria");
        String unidad = normalizarUnidad(serieDiaria.unidad());
        if (!unidad.equals("mm/dia") && !unidad.equals("mm/day")) {
            throw new IllegalArgumentException(
                    "La serie diaria debe estar expresada en mm/dia: " + serieDiaria.unidad());
        }
        if (serieDiaria.puntos().isEmpty()) {
            return new SerieTemporal(serieDiaria.variableId(), "mm/mes", List.of());
        }

        Map<YearMonth, List<PuntoTemporal>> puntosPorMes = new HashMap<>();
        for (PuntoTemporal punto : serieDiaria.puntos()) {
            YearMonth mes = YearMonth.from(punto.fecha());
            puntosPorMes.computeIfAbsent(mes, ignorado -> new ArrayList<>()).add(punto);
        }

        YearMonth primerMes = YearMonth.from(serieDiaria.puntos().getFirst().fecha());
        YearMonth ultimoMes = YearMonth.from(serieDiaria.puntos().getLast().fecha());
        List<PuntoTemporal> puntosMensuales = new ArrayList<>();

        for (YearMonth mes = primerMes; !mes.isAfter(ultimoMes); mes = mes.plusMonths(1)) {
            List<PuntoTemporal> puntosDelMes = puntosPorMes.getOrDefault(mes, List.of());
            double acumulado = 0.0;
            int diasValidos = 0;
            boolean mesCompleto = puntosDelMes.size() == mes.lengthOfMonth();

            for (int indice = 0; indice < puntosDelMes.size(); indice++) {
                PuntoTemporal punto = puntosDelMes.get(indice);
                boolean fechaEsperada = punto.fecha().equals(mes.atDay(indice + 1));
                if (!fechaEsperada || punto.parcial() || punto.esFaltante()) {
                    mesCompleto = false;
                }
                if (!punto.esFaltante()) {
                    acumulado += punto.valor();
                    diasValidos++;
                }
            }

            if (diasValidos == 0) {
                acumulado = Double.NaN;
            }
            puntosMensuales.add(new PuntoTemporal(
                    mes.atDay(1),
                    acumulado,
                    !mesCompleto));
        }

        return new SerieTemporal(serieDiaria.variableId(), "mm/mes", puntosMensuales);
    }

    public SerieTemporal convertirMmHoraAMmMes(SerieTemporal serieMensual) {
        Objects.requireNonNull(serieMensual, "La serie mensual es obligatoria");
        String unidad = normalizarUnidad(serieMensual.unidad());
        if (!unidad.equals("mm/hr") && !unidad.equals("mm/h") && !unidad.equals("mm/hour")) {
            throw new IllegalArgumentException(
                    "La serie mensual debe estar expresada en mm/hr: " + serieMensual.unidad());
        }

        List<PuntoTemporal> puntosConvertidos = serieMensual.puntos().stream()
                .map(punto -> {
                    double valorMensual = punto.esFaltante()
                            ? Double.NaN
                            : punto.valor() * YearMonth.from(punto.fecha()).lengthOfMonth() * 24.0;
                    return new PuntoTemporal(punto.fecha(), valorMensual, punto.parcial());
                })
                .toList();
        return new SerieTemporal(serieMensual.variableId(), "mm/mes", puntosConvertidos);
    }

    private static String normalizarUnidad(String unidad) {
        String sinAcentos = Normalizer.normalize(unidad, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
