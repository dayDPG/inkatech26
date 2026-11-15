package inkatech.detective.web;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.data.AgregadorMensual;
import inkatech.detective.data.RepositorioSeries;
import inkatech.detective.domain.SerieTemporal;
import inkatech.detective.domain.Tendencia;
import inkatech.detective.estadistica.AnalizadorTendencia;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public final class DetectiveController {
    private final CatalogoVariables catalogo;
    private final RepositorioSeries repositorio;
    private final AgregadorMensual agregador;
    private final AnalizadorTendencia analizador;

    public DetectiveController(CatalogoVariables catalogo, RepositorioSeries repositorio,
            AgregadorMensual agregador, AnalizadorTendencia analizador) {
        this.catalogo = catalogo;
        this.repositorio = repositorio;
        this.agregador = agregador;
        this.analizador = analizador;
    }

    @GetMapping("/variables")
    public List<VariableConfig> variables() { return catalogo.listar(); }

    @GetMapping("/serie")
    public SerieTemporal serie(@RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        VariableConfig variable = buscar(var);
        return filtrar(repositorio.obtener(variable), variable, desde, hasta);
    }

    @GetMapping("/tendencia")
    public Tendencia tendencia(@RequestParam String var,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        VariableConfig variable = buscar(var);
        SerieTemporal selected = filtrar(repositorio.obtener(variable), variable, desde, hasta);
        if (variable.esDiaria()) selected = agregador.sumar(selected);
        return analizador.calcular(selected);
    }

    private VariableConfig buscar(String id) {
        return catalogo.buscar(id).orElseThrow(() -> new VariableNoEncontradaException(id));
    }

    private static SerieTemporal filtrar(SerieTemporal serie, VariableConfig variable,
            LocalDate desde, LocalDate hasta) {
        LocalDate start = desde == null ? variable.periodo().desde() : desde;
        LocalDate end = hasta == null ? variable.periodo().hasta() : hasta;
        return serie.filtrar(start, end);
    }
}
