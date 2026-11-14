package inkatech.detective.estadistica;

public record ResultadoMannKendall(long s, double varianza, double z, double valorP) {

    public ResultadoMannKendall {
        if (!Double.isFinite(varianza) || varianza < 0.0) {
            throw new IllegalArgumentException("La varianza debe ser finita y no negativa");
        }
        if (!Double.isFinite(z)) {
            throw new IllegalArgumentException("El estadistico Z debe ser finito");
        }
        if (!Double.isFinite(valorP) || valorP < 0.0 || valorP > 1.0) {
            throw new IllegalArgumentException("El valor p debe estar entre 0 y 1");
        }
    }
}
