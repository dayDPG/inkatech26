package inkatech.detective.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record SerieTemporal(String variableId, String unidad, List<PuntoTemporal> puntos) {

    public SerieTemporal {
        variableId = requireText(variableId, "El identificador de la variable es obligatorio");
        unidad = requireText(unidad, "La unidad es obligatoria");
        puntos = copiarSinNulos(puntos);

        validateChronologicalOrder(puntos);
    }

    public List<LocalDate> fechas() {
        return puntos.stream().map(PuntoTemporal::fecha).toList();
    }

    public double[] valores() {
        return puntos.stream().mapToDouble(PuntoTemporal::valor).toArray();
    }

    public SerieTemporal entre(LocalDate desde, LocalDate hasta) { // pa crear serie entre fechas
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }

        return conPuntos(puntos.stream()
                .filter(punto -> !punto.fecha().isBefore(desde) && !punto.fecha().isAfter(hasta))
                .toList());
    }

    public SerieTemporal filtrarPorRango(LocalDate desde, LocalDate hasta) {
        return entre(desde, hasta);
    }

    public SerieTemporal sinFaltantes() { //elimina los NoN
        return conPuntos(puntos.stream()
                .filter(punto -> !punto.esFaltante())
                .toList());
    }

    public SerieTemporal sinParciales() { //elimina los parciales
        return conPuntos(puntos.stream()
                .filter(punto -> !punto.parcial())
                .toList());
    }

    private SerieTemporal conPuntos(List<PuntoTemporal> nuevosPuntos) {//serie nueva para puntos filtrados
        return new SerieTemporal(variableId, unidad, nuevosPuntos);
    }

    private static String requireText(String value, String message) { 
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static List<PuntoTemporal> copiarSinNulos(List<PuntoTemporal> puntos) { //revisa que no haya nulos
        Objects.requireNonNull(puntos, "La lista de puntos es obligatoria");
        for (PuntoTemporal punto : puntos) {
            Objects.requireNonNull(punto, "La serie no puede contener puntos nulos");
        }

        return List.copyOf(puntos);
    }

    private static void validateChronologicalOrder(List<PuntoTemporal> puntos) { 
        for (int index = 1; index < puntos.size(); index++) {
            LocalDate actual = puntos.get(index).fecha();
            LocalDate anterior = puntos.get(index - 1).fecha();

            if (!actual.isAfter(anterior)) {
                throw new IllegalArgumentException(
                        "Los puntos deben estar ordenados por fecha y no pueden repetirse");
            }
        }
    }
}