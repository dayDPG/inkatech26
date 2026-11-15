package inkatech.detective.stats;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public final class AnalizadorTendencia {

    public static final int MINIMO_MESES = 24;
    private static final int MESES_POR_DECADA = 120;

    private AnalizadorTendencia() {
    }

    public static Optional<Tendencia> analizar(SerieTemporal serieMensual) {
        List<PuntoTemporal> puntos = serieMensual.sinParciales().sinFaltantes().puntos();
        int numeroPuntos = puntos.size();
        if (numeroPuntos < MINIMO_MESES) {
            return Optional.empty();
        }

        YearMonth inicio = YearMonth.from(puntos.get(0).fecha());
        double[] mesesDesdeInicio = new double[numeroPuntos];
        double[] valores = new double[numeroPuntos];
        for (int indice = 0; indice < numeroPuntos; indice++) {
            mesesDesdeInicio[indice] = ChronoUnit.MONTHS.between(inicio, YearMonth.from(puntos.get(indice).fecha()));
            valores[indice] = puntos.get(indice).valor();
            if (indice > 0 && mesesDesdeInicio[indice] <= mesesDesdeInicio[indice - 1]) {
                throw new IllegalArgumentException(
                        "La serie debe ser mensual: un solo punto por mes");
            }
        }

        TheilSen.Resultado sen = TheilSen.estimar(mesesDesdeInicio, valores, Tendencia.NIVEL_CONFIANZA);
        MannKendall.Resultado mk = MannKendall.test(valores);

        return Optional.of(new Tendencia(
                sen.pendiente() * MESES_POR_DECADA,
                mk.valorP(),
                sen.limiteInferior() * MESES_POR_DECADA,
                sen.limiteSuperior() * MESES_POR_DECADA,
                numeroPuntos));
    }
}