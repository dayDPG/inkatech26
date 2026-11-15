package inkatech.detective.web;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.data.RepositorioSeries;
import inkatech.detective.data.SerieTemporal;
import inkatech.detective.service.ServicioAnalisis;
import inkatech.detective.stats.AnalizadorAnalogias;
import inkatech.detective.stats.Correlacion;
import inkatech.detective.stats.AnalizadorEventoActual;
import inkatech.detective.stats.AnalizadorTendencia;
import inkatech.detective.stats.AnalizadorValidacionLluvia;
import inkatech.detective.stats.ResultadoCorrelacion;
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
    private final Correlacion correlacion;
    private final AnalizadorEventoActual analizadorEventoActual;
    private final AnalizadorValidacionLluvia analizadorValidacionLluvia;
    private final AnalizadorAnalogias analizadorAnalogias;
    private final ServicioAnalisis servicioAnalisis;

    public DetectiveController(
            CatalogoVariables catalogoVariables,
            RepositorioSeries repositorioSeries,
            Correlacion correlacion,
            AnalizadorEventoActual analizadorEventoActual,
            AnalizadorValidacionLluvia analizadorValidacionLluvia,
            AnalizadorAnalogias analizadorAnalogias,
            ServicioAnalisis servicioAnalisis) {
        this.catalogoVariables = catalogoVariables;
        this.repositorioSeries = repositorioSeries;
        this.correlacion = correlacion;
        this.analizadorEventoActual = analizadorEventoActual;
        this.analizadorValidacionLluvia = analizadorValidacionLluvia;
        this.analizadorAnalogias = analizadorAnalogias;
        this.servicioAnalisis = servicioAnalisis;
    }

    @GetMapping("/variables")
    public List<VariableConfig> listarVariables() {
        return catalogoVariables.obtenerTodas();
    }

    @GetMapping("/analisis")
    public ResponseEntity<AnalisisDTO> analizar(
            @RequestParam(required = false) String var,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(servicioAnalisis.analizar(var, desde, hasta));
    }

    @GetMapping("/serie")
    public ResponseEntity<SerieTemporal> obtenerSerie(
            @RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(obtenerSerieEnRango(var, desde, hasta));
    }

    @GetMapping("/tendencia")
    public ResponseEntity<Tendencia> obtenerTendencia(
            @RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        Tendencia tendencia = AnalizadorTendencia.analizar(obtenerSerieEnRango(var, desde, hasta))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay suficientes datos para calcular la tendencia de la variable: " + var));

        return ResponseEntity.ok(tendencia);
    }

    private SerieTemporal obtenerSerieEnRango(String variableId, LocalDate desde, LocalDate hasta) {
        VariableConfig config = catalogoVariables.obtenerPorId(variableId)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + variableId));
        SerieTemporal serie = repositorioSeries.obtener(config);
        if (desde == null && hasta == null) return serie;

        LocalDate fechaDesde = desde != null ? desde : serie.fechas().getFirst();
        LocalDate fechaHasta = hasta != null ? hasta : serie.fechas().getLast();
        return serie.filtrarPorRango(fechaDesde, fechaHasta);
    }

    @GetMapping("/correlacion")
    public ResponseEntity<ResultadoCorrelacion> obtenerCorrelacion(
            @RequestParam String a,
            @RequestParam String b,
            @RequestParam int rezago) {
        VariableConfig variableA = catalogoVariables.obtenerPorId(a)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + a));
        VariableConfig variableB = catalogoVariables.obtenerPorId(b)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + b));

        if (!esRelacionPermitida(variableA.grupo(), variableB.grupo())) {
            throw new IllegalArgumentException(
                    "La correlación debe ser viento → mar, mar → lluvia o mar → clorofila");
        }

        ResultadoCorrelacion resultado = correlacion.calcular(
                repositorioSeries.obtener(variableA),
                repositorioSeries.obtener(variableB),
                rezago);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/evento")
    public ResponseEntity<AnalizadorEventoActual.Resultado> obtenerEventoActual(
            @RequestParam String var) {
        VariableConfig config = catalogoVariables.obtenerPorId(var)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + var));
        return ResponseEntity.ok(analizadorEventoActual.analizar(repositorioSeries.obtener(config)));
    }

    @GetMapping("/validacion-lluvia")
    public ResponseEntity<AnalizadorValidacionLluvia.Resultado> validarLluvia() {
        VariableConfig late = catalogoVariables.obtenerPorId("precipitation-norte")
                .orElseThrow(() -> new IllegalStateException("Falta la serie de lluvia Late del norte"));
        VariableConfig finalMensual = catalogoVariables.obtenerPorId("precipitation-norte-final")
                .orElseThrow(() -> new IllegalStateException("Falta la serie de lluvia Final del norte"));

        return ResponseEntity.ok(analizadorValidacionLluvia.analizar(
                repositorioSeries.obtener(late),
                repositorioSeries.obtener(finalMensual)));
    }

    @GetMapping("/analogias")
    public ResponseEntity<AnalizadorAnalogias.Resultado> analogiasHistoricas() {
        VariableConfig temperatura = catalogoVariables.obtenerPorId("sea-surface-temperature-total")
                .orElseThrow(() -> new IllegalStateException("Falta la serie de temperatura del mar total"));
        VariableConfig clorofila = catalogoVariables.obtenerPorId("chlorophyll-a-total")
                .orElseThrow(() -> new IllegalStateException("Falta la serie de clorofila total"));

        return ResponseEntity.ok(analizadorAnalogias.analizar(
                repositorioSeries.obtener(temperatura),
                repositorioSeries.obtener(clorofila)));
    }

    private static boolean esRelacionPermitida(String grupoA, String grupoB) {
        return ("eastward-wind".equals(grupoA) && "sea-surface-temperature".equals(grupoB))
                || ("sea-surface-temperature".equals(grupoA)
                        && ("precipitation".equals(grupoB) || "chlorophyll-a".equals(grupoB)));
    }
}

