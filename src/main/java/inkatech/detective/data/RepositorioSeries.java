package inkatech.detective.data;

import inkatech.detective.config.VariableConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RepositorioSeries {

    public SerieTemporal obtener(VariableConfig variable) {
        Objects.requireNonNull(variable, "La variable es obligatoria");
        try {
            Path ruta = resolverRuta(variable.archivo());
            return GiovanniCsvReader.leer(ruta, variable);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la serie: " + variable.id(), e);
        }
    }

    public Optional<SerieTemporal> obtenerPorId(String id, List<VariableConfig> catalogo) {
        Objects.requireNonNull(catalogo, "El catálogo es obligatorio");
        return catalogo.stream()
                .filter(variable -> variable.id().equals(id))
                .findFirst()
                .map(this::obtener);
    }

    private static Path resolverRuta(String archivo) {
        if (archivo == null || archivo.isBlank()) {
            throw new IllegalArgumentException("La ruta del archivo es obligatoria");
        }

        List<Path> rutas = new ArrayList<>();
        rutas.add(Paths.get(archivo));
        rutas.add(Paths.get("", archivo).toAbsolutePath().normalize());

        Path actual = Paths.get("").toAbsolutePath().normalize();
        rutas.add(actual.resolve(archivo));
        rutas.add(actual.resolve("src/main/java").resolve(archivo));
        rutas.add(actual.resolve("src/main/resources").resolve(archivo));

        String nombreArchivo = Paths.get(archivo).getFileName().toString();
        try {
            Path buscado = Files.walk(actual, 6)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(nombreArchivo))
                    .min(Comparator.comparingInt(path -> path.toString().length()))
                    .orElse(null);
            if (buscado != null) {
                rutas.add(buscado);
            }
        } catch (IOException ignored) {
            // Si no se puede hacer búsqueda recursiva, se ignora y se usa la ruta directa.
        }

        for (Path ruta : rutas) {
            if (Files.isRegularFile(ruta)) {
                return ruta.toAbsolutePath().normalize();
            }
        }

        return Paths.get(archivo).toAbsolutePath().normalize();
    }
}
