package inkatech.detective.stats;

public record ResultadoCorrelacion(
        double coeficiente,
        double valorP,
        int rezagoMeses,
        int numeroPares) {

    public ResultadoCorrelacion {
        if (!Double.isFinite(coeficiente) || coeficiente < -1.0 || coeficiente > 1.0) {
            throw new IllegalArgumentException("El coeficiente debe estar entre -1 y 1");
        }
        if (!Double.isFinite(valorP) || valorP < 0.0 || valorP > 1.0) {
            throw new IllegalArgumentException("El valor p debe estar entre 0 y 1");
        }
        if (rezagoMeses < 0 || rezagoMeses > 4) {
            throw new IllegalArgumentException("El rezago debe estar entre 0 y 4 meses");
        }
        if (numeroPares < 3) {
            throw new IllegalArgumentException("Se requieren al menos tres pares validos");
        }
    }
}
