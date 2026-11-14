package inkatech.detective.estadistica;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TheilSen {

    private static final double YEARS_PER_DECADE = 10.0;

    public ResultadoTheilSen calcular(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");

        List<PuntoTemporal> validPoints = serie.puntos().stream()
                .filter(point -> Double.isFinite(point.valor()))
                .toList();
        if (validPoints.size() < 2) {
            throw new IllegalArgumentException(
                    "Theil-Sen requiere al menos dos observaciones validas");
        }

        List<Double> slopes = calculatePairSlopes(validPoints);
        Collections.sort(slopes);
        double annualSlope = median(slopes);

        return new ResultadoTheilSen(
                annualSlope,
                annualSlope * YEARS_PER_DECADE,
                slopes.size());
    }

    static double decimalYear(LocalDate date) {
        return date.getYear()
                + (date.getDayOfYear() - 1.0) / date.lengthOfYear();
    }

    private static List<Double> calculatePairSlopes(List<PuntoTemporal> points) {
        int pairCount = points.size() * (points.size() - 1) / 2;
        List<Double> slopes = new ArrayList<>(pairCount);

        for (int first = 0; first < points.size() - 1; first++) {
            PuntoTemporal firstPoint = points.get(first);
            for (int second = first + 1; second < points.size(); second++) {
                PuntoTemporal secondPoint = points.get(second);
                double elapsedYears = decimalYear(secondPoint.fecha())
                        - decimalYear(firstPoint.fecha());
                slopes.add((secondPoint.valor() - firstPoint.valor()) / elapsedYears);
            }
        }

        return slopes;
    }

    private static double median(List<Double> sortedValues) {
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return sortedValues.get(middle);
        }
        return (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0;
    }
}
