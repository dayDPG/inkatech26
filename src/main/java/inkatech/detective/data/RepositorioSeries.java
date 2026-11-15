package inkatech.detective.data;

import inkatech.detective.config.VariableConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RepositorioSeries {

    private final AgregadorMensual agregadorMensual;
    private final Map<String, SerieTemporal> cache = new ConcurrentHashMap<>();

    public RepositorioSeries(AgregadorMensual agregadorMensual) {
        this.agregadorMensual = Objects.requireNonNull(agregadorMensual);
    }

    public SerieTemporal obtener(VariableConfig variable) {
        Objects.requireNonNull(variable, "La variable es obligatoria");
        return cache.computeIfAbsent(variable.id(), ignored -> cargar(variable));
    }

    private SerieTemporal cargar(VariableConfig variable) {
        try {
            Path ruta = resolverRuta(variable.archivo());
            SerieTemporal serie;
            if (Files.isRegularFile(ruta)) {
                serie = GiovanniCsvReader.leer(ruta, variable);
            } else {
                String nombreRecurso = "inkatech/detective/csv/"
                        + Paths.get(variable.archivo()).getFileName();
                InputStream recurso = RepositorioSeries.class.getClassLoader()
                        .getResourceAsStream(nombreRecurso);
                if (recurso == null) {
                    throw new IllegalArgumentException("No se encontró el CSV: " + variable.archivo());
                }
                try (recurso) {
                    serie = GiovanniCsvReader.leer(recurso, variable);
                }
            }
            if (variable.grupo().equals("precipitation")) {
                return agregadorMensual.normalizarPrecipitacionMensual(serie);
            }
            return variable.esDiaria()
                    ? serie
                    : AgregadorMensual.promediarPorMes(serie);
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

        for (Path ruta : rutas) {
            if (Files.isRegularFile(ruta)) {
                return ruta.toAbsolutePath().normalize();
            }
        }

        return Paths.get(archivo).toAbsolutePath().normalize();
    }
}
