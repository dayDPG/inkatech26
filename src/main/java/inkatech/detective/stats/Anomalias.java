package inkatech.detective.stats;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Anomalias {

    private static final int MESES_POR_ANO = 12;

    public SerieTemporal calcular(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");

        double[] sumas = new double[MESES_POR_ANO];
        int[] observaciones = new int[MESES_POR_ANO];

        for (PuntoTemporal punto : serie.puntos()) {
            if (Double.isFinite(punto.valor())) {
                int indiceMes = punto.fecha().getMonthValue() - 1;
                sumas[indiceMes] += punto.valor();
                observaciones[indiceMes]++;
            }
        }

        List<PuntoTemporal> anomalias = new ArrayList<>(serie.puntos().size());
        for (PuntoTemporal punto : serie.puntos()) {
            double anomalia = calcularAnomalia(punto, sumas, observaciones);
            anomalias.add(new PuntoTemporal(punto.fecha(), anomalia));
        }

        return new SerieTemporal(serie.variableId(), serie.unidad(), anomalias);
    }

    private static double calcularAnomalia(
            PuntoTemporal punto,
            double[] sumas,
            int[] observaciones) {
        if (Double.isNaN(punto.valor())) {
            return Double.NaN;
        }

        int indiceMes = punto.fecha().getMonthValue() - 1;
        if (observaciones[indiceMes] == 0) {
            return Double.NaN;
        }

        return punto.valor() - sumas[indiceMes] / observaciones[indiceMes];
    }
}
