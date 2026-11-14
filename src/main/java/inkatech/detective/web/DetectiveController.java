package inkatech.detective.web;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.data.RepositorioSeries;
import inkatech.detective.data.SerieTemporal;
import inkatech.detective.stats.AnalizadorTendencia;
import inkatech.detective.stats.Tendencia;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DetectiveController {

    private final CatalogoVariables catalogoVariables;
    private final RepositorioSeries repositorioSeries;
    private final AnalizadorTendencia analizadorTendencia;

    public DetectiveController(
            CatalogoVariables catalogoVariables,
            RepositorioSeries repositorioSeries,
            AnalizadorTendencia analizadorTendencia) {
        this.catalogoVariables = catalogoVariables;
        this.repositorioSeries = repositorioSeries;
        this.analizadorTendencia = analizadorTendencia;
    }

    @GetMapping("/variables")
    public List<VariableConfig> listarVariables() {
        return catalogoVariables.obtenerTodas();
    }

    @GetMapping("/serie")
    public ResponseEntity<SerieTemporal> obtenerSerie(
            @RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        VariableConfig config = catalogoVariables.obtenerPorId(var)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + var));

        SerieTemporal serie = repositorioSeries.obtener(config);
        if (desde != null || hasta != null) {
            LocalDate fechaDesde = desde != null ? desde : serie.fechas().getFirst();
            LocalDate fechaHasta = hasta != null ? hasta : serie.fechas().getLast();
            serie = serie.filtrarPorRango(fechaDesde, fechaHasta);
        }

        return ResponseEntity.ok(serie);
    }

    @GetMapping("/tendencia")
    public ResponseEntity<Tendencia> obtenerTendencia(
            @RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        VariableConfig config = catalogoVariables.obtenerPorId(var)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + var));

        SerieTemporal serie = repositorioSeries.obtener(config);
        if (desde != null || hasta != null) {
            LocalDate fechaDesde = desde != null ? desde : serie.fechas().getFirst();
            LocalDate fechaHasta = hasta != null ? hasta : serie.fechas().getLast();
            serie = serie.filtrarPorRango(fechaDesde, fechaHasta);
        }

        Tendencia tendencia = analizadorTendencia.analizar(serie)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay suficientes datos para calcular la tendencia de la variable: " + var));

        return ResponseEntity.ok(tendencia);
    }
}

