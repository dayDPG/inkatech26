package inkatech.detective.web;

import inkatech.detective.data.SerieTemporal;
import inkatech.detective.stats.Tendencia;
import java.time.LocalDate;
import java.util.List;

public record AnalisisDTO(
        SerieTemporal serie,
        SerieTemporal anomalias,
        Tendencia tendencia,
        String mensajeTendencia,
        Evento evento,
        List<Relacion> relaciones) {

    public AnalisisDTO {
        relaciones = List.copyOf(relaciones);
    }

    public record Evento(
            LocalDate fecha,
            double valor,
            Double zScore,
            Double percentil,
            int numeroDeMesesHistoricos,
            String mensaje) {}

    public record Relacion(
            String nombre,
            String variableA,
            String variableB,
            List<Rezago> rezagos,
            Integer mejorRezago) {
        public Relacion {
            rezagos = List.copyOf(rezagos);
        }
    }

    public record Rezago(
            int meses,
            Double rho,
            Double valorP,
            int numeroDePares,
            String mensaje) {}
}
