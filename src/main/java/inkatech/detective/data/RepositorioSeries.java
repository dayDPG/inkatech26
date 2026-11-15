package inkatech.detective.data;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.domain.SerieTemporal;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public final class RepositorioSeries {
    private final Map<String, SerieTemporal> series;

    public RepositorioSeries(CatalogoVariables catalogo, ResourceLoader resources) {
        GiovanniCsvReader reader = new GiovanniCsvReader();
        Map<String, SerieTemporal> loaded = new LinkedHashMap<>();
        for (VariableConfig variable : catalogo.listar()) {
            Resource resource = resources.getResource("classpath:" + variable.archivo());
            try (InputStream input = resource.getInputStream()) {
                loaded.put(variable.id(), reader.leer(input, variable));
            } catch (IOException | RuntimeException exception) {
                throw new IllegalStateException("No se pudo precargar " + variable.id()
                        + " desde " + variable.archivo(), exception);
            }
        }
        series = Collections.unmodifiableMap(loaded);
    }

    public SerieTemporal obtener(VariableConfig variable) {
        return obtener(variable.id());
    }

    public SerieTemporal obtener(String id) {
        SerieTemporal serie = series.get(id);
        if (serie == null) throw new IllegalArgumentException("Variable no encontrada: " + id);
        return serie;
    }

    public int cantidadCargada() { return series.size(); }
}
