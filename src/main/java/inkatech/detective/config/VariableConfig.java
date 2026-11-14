package inkatech.detective.config;

import java.util.Objects;

public record VariableConfig(
        String id,
        String nombre,
        String definicion,
        String unidad,
        RegionGeografica region,
        String dataset,
        String expectativaElNino,
        String archivo) {

    public VariableConfig {
        id = requireText(id, "El identificador es obligatorio");
        nombre = requireText(nombre, "El nombre es obligatorio");
        definicion = requireText(definicion, "La definicion es obligatoria");
        unidad = requireText(unidad, "La unidad es obligatoria");
        region = Objects.requireNonNull(region, "La region es obligatoria");
        dataset = requireText(dataset, "El dataset es obligatorio");
        expectativaElNino = requireText(
                expectativaElNino,
                "La expectativa para El Nino es obligatoria");
        archivo = requireText(archivo, "El archivo es obligatorio");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
