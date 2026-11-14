package inkatech.detective.domain;

public record Tendencia(
        double pendientePorDecada,
        double valorP,
        double intervaloConfianzaInferior,
        double intervaloConfianzaSuperior) {

    public static final double NIVEL_SIGNIFICANCIA = 0.05;

    public Tendencia {
        requireFinite(pendientePorDecada, "La pendiente por decada");
        requireFinite(valorP, "El valor p");
        requireFinite(intervaloConfianzaInferior, "El limite inferior");
        requireFinite(intervaloConfianzaSuperior, "El limite superior");

        if (valorP < 0.0 || valorP > 1.0) {
            throw new IllegalArgumentException("El valor p debe estar entre 0 y 1");
        }
        if (intervaloConfianzaInferior > intervaloConfianzaSuperior) {
            throw new IllegalArgumentException(
                    "El limite inferior no puede ser mayor que el limite superior");
        }
    }

    public boolean isSignificativa() {
        return valorP <= NIVEL_SIGNIFICANCIA;
    }

    private static void requireFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " debe ser un numero finito");
        }
    }
}
