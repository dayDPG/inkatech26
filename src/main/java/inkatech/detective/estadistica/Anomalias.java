package inkatech.detective.estadistica;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Anomalias {

    private static final int MONTHS_PER_YEAR = 12;

    public SerieTemporal calcular(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");

        double[] sums = new double[MONTHS_PER_YEAR];
        int[] observations = new int[MONTHS_PER_YEAR];

        for (PuntoTemporal point : serie.puntos()) {
            if (Double.isFinite(point.valor())) {
                int monthIndex = point.fecha().getMonthValue() - 1;
                sums[monthIndex] += point.valor();
                observations[monthIndex]++;
            }
        }

        List<PuntoTemporal> anomalies = new ArrayList<>(serie.puntos().size());
        for (PuntoTemporal point : serie.puntos()) {
            double anomaly = calculateAnomaly(point, sums, observations);
            anomalies.add(new PuntoTemporal(point.fecha(), anomaly));
        }

        return new SerieTemporal(serie.variableId(), serie.unidad(), anomalies);
    }

    private static double calculateAnomaly(
            PuntoTemporal point,
            double[] sums,
            int[] observations) {
        if (Double.isNaN(point.valor())) {
            return Double.NaN;
        }

        int monthIndex = point.fecha().getMonthValue() - 1;
        if (observations[monthIndex] == 0) {
            return Double.NaN;
        }

        return point.valor() - sums[monthIndex] / observations[monthIndex];
    }
}
