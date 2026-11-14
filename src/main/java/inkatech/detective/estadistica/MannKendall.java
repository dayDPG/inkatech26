package inkatech.detective.estadistica;

import inkatech.detective.domain.SerieTemporal;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.math3.distribution.NormalDistribution;

public final class MannKendall {

    private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution();

    public ResultadoMannKendall calcular(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");

        double[] values = serie.puntos().stream()
                .mapToDouble(point -> point.valor())
                .filter(Double::isFinite)
                .toArray();

        if (values.length < 2) {
            throw new IllegalArgumentException(
                    "Mann-Kendall requiere al menos dos observaciones validas");
        }

        long s = calculateS(values);
        double variance = calculateVariance(values);
        double z = calculateZ(s, variance);
        double pValue = 2.0 * STANDARD_NORMAL.cumulativeProbability(-Math.abs(z));

        return new ResultadoMannKendall(s, variance, z, pValue);
    }

    private static long calculateS(double[] values) {
        long s = 0;
        for (int first = 0; first < values.length - 1; first++) {
            for (int second = first + 1; second < values.length; second++) {
                s += Double.compare(values[second], values[first]);
            }
        }
        return s;
    }

    private static double calculateVariance(double[] values) {
        long observationCount = values.length;
        double numerator = observationCount
                * (observationCount - 1)
                * (2 * observationCount + 5);

        double[] sortedValues = values.clone();
        Arrays.sort(sortedValues);

        int runLength = 1;
        for (int index = 1; index <= sortedValues.length; index++) {
            if (index < sortedValues.length
                    && Double.compare(sortedValues[index], sortedValues[index - 1]) == 0) {
                runLength++;
                continue;
            }

            if (runLength > 1) {
                numerator -= (double) runLength
                        * (runLength - 1)
                        * (2 * runLength + 5);
            }
            runLength = 1;
        }

        return numerator / 18.0;
    }

    private static double calculateZ(long s, double variance) {
        if (s == 0 || variance == 0.0) {
            return 0.0;
        }
        if (s > 0) {
            return (s - 1.0) / Math.sqrt(variance);
        }
        return (s + 1.0) / Math.sqrt(variance);
    }
}
