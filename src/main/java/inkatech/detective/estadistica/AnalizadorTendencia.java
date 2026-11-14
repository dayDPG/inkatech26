package inkatech.detective.estadistica;

import inkatech.detective.domain.SerieTemporal;
import inkatech.detective.domain.Tendencia;
import java.util.Objects;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Combina una pendiente robusta Theil-Sen, significancia Mann-Kendall y un
 * intervalo de confianza del 95 % obtenido mediante regresion lineal.
 */
public final class AnalizadorTendencia {

    private static final double YEARS_PER_DECADE = 10.0;
    private static final double CONFIDENCE_QUANTILE = 0.975;

    private final Anomalias anomalias;
    private final MannKendall mannKendall;
    private final TheilSen theilSen;

    public AnalizadorTendencia() {
        this(new Anomalias(), new MannKendall(), new TheilSen());
    }

    AnalizadorTendencia(Anomalias anomalias, MannKendall mannKendall, TheilSen theilSen) {
        this.anomalias = Objects.requireNonNull(anomalias);
        this.mannKendall = Objects.requireNonNull(mannKendall);
        this.theilSen = Objects.requireNonNull(theilSen);
    }

    public Tendencia calcular(SerieTemporal serie) {
        SerieTemporal anomalySeries = anomalias.calcular(
                Objects.requireNonNull(serie, "La serie temporal es obligatoria"));

        long observationCount = anomalySeries.puntos().stream()
                .filter(point -> Double.isFinite(point.valor()))
                .count();
        if (observationCount < 3) {
            throw new IllegalArgumentException(
                    "La tendencia requiere al menos tres observaciones validas");
        }

        ResultadoMannKendall mannKendallResult = mannKendall.calcular(anomalySeries);
        ResultadoTheilSen theilSenResult = theilSen.calcular(anomalySeries);
        ConfidenceInterval confidenceInterval = calculateConfidenceInterval(anomalySeries);

        return new Tendencia(
                theilSenResult.pendientePorDecada(),
                mannKendallResult.valorP(),
                confidenceInterval.lower(),
                confidenceInterval.upper());
    }

    private static ConfidenceInterval calculateConfidenceInterval(SerieTemporal serie) {
        SimpleRegression regression = new SimpleRegression(true);
        serie.puntos().stream()
                .filter(point -> Double.isFinite(point.valor()))
                .forEach(point -> regression.addData(
                        TheilSen.decimalYear(point.fecha()),
                        point.valor()));

        double annualSlope = regression.getSlope();
        double annualStandardError = regression.getSlopeStdErr();
        int degreesOfFreedom = (int) regression.getN() - 2;
        double criticalValue = new TDistribution(degreesOfFreedom)
                .inverseCumulativeProbability(CONFIDENCE_QUANTILE);
        double marginPerDecade = criticalValue * annualStandardError * YEARS_PER_DECADE;
        double slopePerDecade = annualSlope * YEARS_PER_DECADE;

        return new ConfidenceInterval(
                slopePerDecade - marginPerDecade,
                slopePerDecade + marginPerDecade);
    }

    private record ConfidenceInterval(double lower, double upper) {
    }
}
