package inkatech.detective.stats;

public record ResultadoTheilSen(
        double pendienteAnual,
        double pendientePorDecada,
        int cantidadPendientes, 
        double limiteInferior,
        double limiteSuperior, int puntosUsados
    ) {

    public ResultadoTheilSen {
        if (!Double.isFinite(pendienteAnual) || !Double.isFinite(pendientePorDecada)) {
            throw new IllegalArgumentException("Las pendientes deben ser numeros finitos");
        }
        if (cantidadPendientes < 1) {
            throw new IllegalArgumentException("Debe existir al menos una pendiente");
        }
    }
}