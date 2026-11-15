package inkatech.detective.stats;

import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.math3.distribution.NormalDistribution;

/*ESTABLECIDO CON EL CALCULO FORMAL DE LA PENDIENTE DE THEIL-SEN, CON INTERVALO DE CONFIANZA

la pendiente de una serie nos sirve para estimar la tendencia de las variables climaticas
 util para detectar cambios en la temperatura, precipitacion, etc. a lo largo del tiempo*/

public final class TheilSen {

    public static final int MINIMO_PUNTOS = 4;
    // LIMITE para no gastar memoria en series largas
    public static final int MAXIMO_PUNTOS = 2000;
    public static final double NIVEL_CONFIANZA = 0.95;

    private static final NormalDistribution NORMAL = new NormalDistribution(0.0, 1.0);

    private TheilSen() {
    }

    public static Resultado estimar(double[] x, double[] y) {
        return estimar(x, y, NIVEL_CONFIANZA);
    }

    public static Resultado estimar(double[] valoresX, double[] valoresY, double nivelConfianza) {
        Objects.requireNonNull(valoresX, "Los valores de x son obligatorios");
        Objects.requireNonNull(valoresY, "Los valores de y son obligatorios");
        if (valoresX.length != valoresY.length) {
            throw new IllegalArgumentException("x e y deben tener el mismo tamano");
        }
        int numeroPuntos = valoresX.length;

        if (numeroPuntos < MINIMO_PUNTOS) {
            throw new IllegalArgumentException(
                    "Theil-Sen necesita al menos " + MINIMO_PUNTOS + " puntos");
        }
        if (numeroPuntos > MAXIMO_PUNTOS) {
            throw new IllegalArgumentException(
                    "Demasiados puntos (maximo " + MAXIMO_PUNTOS + "): usar una serie mensual");
        }
        if (!(nivelConfianza > 0.0 && nivelConfianza < 1.0)) {
            throw new IllegalArgumentException("El nivel de confianza debe estar entre 0 y 1");
        }
        for (int indice = 0; indice < numeroPuntos; indice++) {
            if (!Double.isFinite(valoresX[indice]) || !Double.isFinite(valoresY[indice])) {
                throw new IllegalArgumentException(
                        "Los valores deben ser finitos: quitar los faltantes antes de calcular");
            }
        }

        int cantidadMaxima = numeroPuntos * (numeroPuntos - 1) / 2;
        double[] pendientes = new double[cantidadMaxima];
        int cantidad = 0;
        for (int i = 0; i < numeroPuntos - 1; i++) {
            double xInicial = valoresX[i];
            double yInicial = valoresY[i];
            for (int j = i + 1; j < numeroPuntos; j++) {
                double diferenciaX = valoresX[j] - xInicial;
                if (diferenciaX > 0.0) {
                    pendientes[cantidad++] = (valoresY[j] - yInicial) / diferenciaX;
                }
            }
        }
        if (cantidad == 0) {
            throw new IllegalArgumentException(
                    "x no tiene valores distintos: no se puede estimar la pendiente");
        }

        double[] ordenadas = Arrays.copyOf(pendientes, cantidad);
        Arrays.sort(ordenadas);

        double mediana = cantidad % 2 == 1
                ? ordenadas[cantidad / 2]
                : (ordenadas[cantidad / 2 - 1] + ordenadas[cantidad / 2]) / 2.0;

        double varianza = (numeroPuntos * (double) (numeroPuntos - 1) * (2.0 * numeroPuntos + 5.0)
                - MannKendall.sumaEmpates(valoresX)
                - MannKendall.sumaEmpates(valoresY)) / 18.0;
        double sigma = Math.sqrt(Math.max(0.0, varianza));

        double estadisticoZ = NORMAL.inverseCumulativeProbability((1.0 + nivelConfianza) / 2.0);
        double ajuste = estadisticoZ * sigma;

        int indiceInferior = (int) Math.max(0, Math.rint((cantidad - ajuste) / 2.0) - 1);
        int indiceSuperior = (int) Math.min(cantidad - 1, Math.rint((cantidad + ajuste) / 2.0));
        indiceInferior = Math.min(indiceInferior, cantidad - 1);
        indiceSuperior = Math.max(indiceSuperior, 0);

        double inferior = Math.min(ordenadas[indiceInferior], ordenadas[indiceSuperior]);
        double superior = Math.max(ordenadas[indiceInferior], ordenadas[indiceSuperior]);

        return new Resultado(mediana, inferior, superior, numeroPuntos);
    }

    /**
     * @param pendiente      mediana de las pendientes (unidades de y por unidad de
     *                       x)
     * @param limiteInferior extremo inferior del intervalo de confianza
     * @param limiteSuperior extremo superior del intervalo de confianza
     * @param n              cantidad de puntos usados
     */
    public record Resultado(double pendiente, double limiteInferior, double limiteSuperior, int n) {
    }
}