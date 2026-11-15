package inkatech.detective.stats;

import inkatech.detective.data.SerieTemporal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.distribution.NormalDistribution;

/*Detectar tendencia monotona de una serie temporal
sirve para detectar cambios en la temperatura, precipitacion, etc. a lo largo del tiempo*/
public final class MannKendall {

    public static final int MINIMO_PUNTOS = 4;

    private static final NormalDistribution NORMAL = new NormalDistribution(0.0, 1.0);

    private MannKendall() {
    }

    public static Resultado calcular(SerieTemporal serie) {
        Objects.requireNonNull(serie, "La serie temporal es obligatoria");
        Objects.requireNonNull(serie.puntos(), "Los puntos de la serie temporal son obligatorios");

        double[] valores = serie.puntos().stream()
                .filter(punto -> !punto.parcial() && Double.isFinite(punto.valor()))
                .mapToDouble(punto -> punto.valor())
                .toArray();

        return test(valores);
    }

    public static Resultado test(double[] valores) {
        Objects.requireNonNull(valores, "Los valores de la serie son obligatorios");

        if (valores.length < MINIMO_PUNTOS) {
            throw new IllegalArgumentException(
                    "Mann-Kendall requiere al menos " + MINIMO_PUNTOS + " observaciones válidas (encontradas: "
                            + valores.length + ")");
        }

        for (double valor : valores) {
            if (!Double.isFinite(valor)) {
                throw new IllegalArgumentException("Los valores deben ser finitos");
            }
        }

        long estadisticoS = calcularS(valores);
        double varianza = calcularVarianza(valores);
        double estadisticoZ = calcularZ(estadisticoS, varianza);
        double valorP = Math.min(1.0, 2.0 * NORMAL.cumulativeProbability(-Math.abs(estadisticoZ)));

        return new Resultado(valores.length, estadisticoS, varianza, estadisticoZ, valorP);
    }

    private static long calcularS(double[] valores) {
        double[] valoresOrdenados = valores.clone();
        Arrays.sort(valoresOrdenados);

        double[] valoresUnicos = Arrays.stream(valoresOrdenados)
                .distinct()
                .toArray();

        int[] fenwick = new int[valoresUnicos.length + 1];
        long totalObservados = 0L;
        long estadisticoS = 0L;

        for (double valorActual : valores) {
            int indice = Arrays.binarySearch(valoresUnicos, valorActual) + 1;
            long menoresQueActual = consultarFenwick(fenwick, indice - 1);
            long igualesQueActual = consultarFenwick(fenwick, indice) - menoresQueActual;
            long mayoresQueActual = totalObservados - menoresQueActual - igualesQueActual;
            estadisticoS += menoresQueActual - mayoresQueActual;
            actualizarFenwick(fenwick, indice, 1);
            totalObservados++;
        }

        return estadisticoS;
    }

    private static int consultarFenwick(int[] fenwick, int indice) {
        int suma = 0;
        for (int i = indice; i > 0; i -= i & -i) {
            suma += fenwick[i];
        }
        return suma;
    }

    private static void actualizarFenwick(int[] fenwick, int indice, int delta) {
        int longitud = fenwick.length - 1;
        for (int i = indice; i <= longitud; i += i & -i) {
            fenwick[i] += delta;
        }
    }

    private static double calcularVarianza(double[] valores) {
        long cantidadObservaciones = valores.length;
        double numerador = cantidadObservaciones
                * (cantidadObservaciones - 1)
                * (2 * cantidadObservaciones + 5);

        numerador -= sumaEmpates(valores);

        return numerador / 18.0;
    }

    public static double sumaEmpates(double[] valores) {
        if (valores == null || valores.length == 0) {
            return 0.0;
        }

        Map<Double, Integer> frecuencias = new HashMap<>();
        for (double valor : valores) {
            frecuencias.merge(valor, 1, Integer::sum);
        }

        double suma = 0.0;
        for (int frecuencia : frecuencias.values()) {
            if (frecuencia > 1) {
                suma += (double) frecuencia * (frecuencia - 1) * (2L * frecuencia + 5L);
            }
        }
        return suma;
    }

    private static double calcularZ(long estadisticoS, double varianza) {
        if (varianza <= 0.0) {
            return 0.0;
        }
        double sigma = Math.sqrt(varianza);
        if (estadisticoS > 0) {
            return (estadisticoS - 1.0) / sigma;
        }
        if (estadisticoS < 0) {
            return (estadisticoS + 1.0) / sigma;
        }
        return 0.0;
    }

    /**
     * @param n cantidad de puntos
     * @param s estadistico S (pares crecientes menos pares decrecientes)
     * @param varianza varianza de S, corregida por empates
     * @param z estadistico Z con correccion de continuidad
     * @param valorP valor p de dos colas
     */
    public record Resultado(int n, long s, double varianza, double z, double valorP) {

        // 1 si la tendencia es creciente, -1 si es decreciente, 0 si no hay
        public int sentido() {
            if (z > 0) {
                return 1;
            }
            if (z < 0) {
                return -1;
            }
            return 0;
        }
    }
}