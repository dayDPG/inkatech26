package inkatech.detective.estadistica;

public record ResultadoTheilSen(
        double pendienteAnual,
        double pendientePorDecada,
        int cantidadPendientes) {

    public ResultadoTheilSen {
        if (!Double.isFinite(pendienteAnual) || !Double.isFinite(pendientePorDecada)) {
            throw new IllegalArgumentException("Las pendientes deben ser numeros finitos");
        }
        if (cantidadPendientes < 1) {
            throw new IllegalArgumentException("Debe existir al menos una pendiente");
        }
    }
}
