package inkatech.detective.service;

import inkatech.detective.config.CatalogoVariables;
import inkatech.detective.config.VariableConfig;
import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.RepositorioSeries;
import inkatech.detective.data.SerieTemporal;
import inkatech.detective.stats.AnalizadorTendencia;
import inkatech.detective.stats.Anomalias;
import inkatech.detective.stats.Correlacion;
import inkatech.detective.stats.ResultadoCorrelacion;
import inkatech.detective.stats.Tendencia;
import inkatech.detective.web.AnalisisDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ServicioAnalisis {

    private static final int MAXIMO_REZAGO = 4;
    private static final String VIENTO = "eastward-wind-pacifico";
    private static final String TEMPERATURA = "sea-surface-temperature-total";
    private static final String LLUVIA = "precipitation-norte";
    private static final String CLOROFILA = "chlorophyll-a-franja-norte";

    private final CatalogoVariables catalogo;
    private final RepositorioSeries repositorio;
    private final Correlacion correlacion;
    private final Anomalias anomalias = new Anomalias();

    public ServicioAnalisis(
            CatalogoVariables catalogo,
            RepositorioSeries repositorio,
            Correlacion correlacion) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.repositorio = Objects.requireNonNull(repositorio);
        this.correlacion = Objects.requireNonNull(correlacion);
    }

    public AnalisisDTO analizar(String variableId, String desdeTexto, String hastaTexto) {
        LocalDate desde = parsearFecha(desdeTexto, "desde");
        LocalDate hasta = parsearFecha(hastaTexto, "hasta");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }

        VariableConfig variable = catalogo.obtenerPorId(variableId)
                .orElseThrow(() -> new IllegalArgumentException("Variable no encontrada: " + variableId));
        SerieTemporal serie = repositorio.obtener(variable).filtrarPorRango(desde, hasta);
        if (serie.puntos().isEmpty()) {
            throw new IllegalArgumentException("No hay datos de la variable en el periodo seleccionado");
        }

        SerieTemporal anomaliasCalculadas = anomalias.calcular(serie.sinParciales());
        Optional<Tendencia> tendencia = AnalizadorTendencia.analizar(anomaliasCalculadas);
        String mensajeTendencia = tendencia.isPresent()
                ? null
                : "No hay suficientes observaciones completas para calcular la tendencia (se requieren 24 meses).";

        return new AnalisisDTO(
                serie,
                anomaliasCalculadas,
                tendencia.orElse(null),
                mensajeTendencia,
                analizarEvento(serie, repositorio.obtener(variable)),
                calcularRelaciones(variable, desde, hasta));
    }

    private List<AnalisisDTO.Relacion> calcularRelaciones(
            VariableConfig seleccionada,
            LocalDate desde,
            LocalDate hasta) {
        YearMonth primerMes = YearMonth.from(desde);
        YearMonth ultimoMes = YearMonth.from(hasta);
        List<RelacionBase> relaciones = switch (seleccionada.grupo()) {
            case "eastward-wind" -> List.of(new RelacionBase(VIENTO, TEMPERATURA));
            case "sea-surface-temperature" -> List.of(
                    new RelacionBase(VIENTO, TEMPERATURA),
                    new RelacionBase(TEMPERATURA, LLUVIA),
                    new RelacionBase(TEMPERATURA, CLOROFILA));
            case "precipitation" -> List.of(new RelacionBase(TEMPERATURA, seleccionada.id()));
            case "chlorophyll-a" -> List.of(new RelacionBase(TEMPERATURA, seleccionada.id()));
            default -> List.of();
        };

        return relaciones.stream()
                .map(base -> calcularRelacion(base, seleccionada, desde, hasta, primerMes, ultimoMes))
                .toList();
    }

    private AnalisisDTO.Relacion calcularRelacion(
            RelacionBase base,
            VariableConfig seleccionada,
            LocalDate desde,
            LocalDate hasta,
            YearMonth primerMes,
            YearMonth ultimoMes) {
        VariableConfig configA = obtenerConfig(base.variableA(), seleccionada);
        VariableConfig configB = obtenerConfig(base.variableB(), seleccionada);
        SerieTemporal serieA = obtenerSerie(configA, desde, hasta);
        SerieTemporal serieB = obtenerSerie(configB, desde, hasta);
        List<AnalisisDTO.Rezago> rezagos = new ArrayList<>();
        Integer mejorRezago = null;
        double mayorMagnitud = -1.0;

        for (int lag = 0; lag <= MAXIMO_REZAGO; lag++) {
            int numeroDePares = correlacion.contarPares(serieA, serieB, lag, primerMes, ultimoMes);
            if (numeroDePares < 3) {
                rezagos.add(new AnalisisDTO.Rezago(
                        lag, null, null, numeroDePares, "Menos de tres pares válidos."));
                continue;
            }

            try {
                ResultadoCorrelacion resultado =
                        correlacion.calcular(serieA, serieB, lag, primerMes, ultimoMes);
                rezagos.add(new AnalisisDTO.Rezago(
                        lag, resultado.coeficiente(), resultado.valorP(), resultado.numeroPares(), null));
                if (Math.abs(resultado.coeficiente()) > mayorMagnitud) {
                    mayorMagnitud = Math.abs(resultado.coeficiente());
                    mejorRezago = lag;
                }
            } catch (IllegalArgumentException exception) {
                if (!String.valueOf(exception.getMessage()).contains("serie constante")) throw exception;
                rezagos.add(new AnalisisDTO.Rezago(
                        lag, null, null, numeroDePares, "No se puede calcular: una serie no varía."));
            }
        }

        String nombreA = configA.nombre();
        String nombreB = configB.nombre();
        return new AnalisisDTO.Relacion(
                nombreA + " → " + nombreB,
                configA.id(),
                configB.id(),
                rezagos,
                mejorRezago);
    }

    private VariableConfig obtenerConfig(String id, VariableConfig seleccionada) {
        if (seleccionada.id().equals(id)) return seleccionada;
        return catalogo.obtenerPorId(id)
                .orElseThrow(() -> new IllegalStateException("Falta la variable de relación: " + id));
    }

    private SerieTemporal obtenerSerie(
            VariableConfig config,
            LocalDate desde,
            LocalDate hasta) {
        return repositorio.obtener(config).filtrarPorRango(desde, hasta);
    }

    private static AnalisisDTO.Evento analizarEvento(
            SerieTemporal periodoSeleccionado,
            SerieTemporal serieCompleta) {
        PuntoTemporal ultimo = periodoSeleccionado.puntos().stream()
                .filter(punto -> !punto.parcial() && Double.isFinite(punto.valor()))
                .max(Comparator.comparing(PuntoTemporal::fecha))
                .orElse(null);
        if (ultimo == null) return null;

        YearMonth mesUltimo = YearMonth.from(ultimo.fecha());
        List<Double> historicos = serieCompleta.puntos().stream()
                .filter(punto -> !punto.parcial()
                        && Double.isFinite(punto.valor())
                        && punto.fecha().getMonth() == ultimo.fecha().getMonth()
                        && YearMonth.from(punto.fecha()).isBefore(mesUltimo))
                .map(PuntoTemporal::valor)
                .toList();

        if (historicos.size() < 2) {
            return new AnalisisDTO.Evento(
                    ultimo.fecha(), ultimo.valor(), null, null, historicos.size(),
                    "No hay suficientes años anteriores del mismo mes en el periodo.");
        }

        double media = historicos.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double sumaCuadrados = historicos.stream()
                .mapToDouble(valor -> Math.pow(valor - media, 2.0))
                .sum();
        double desviacion = Math.sqrt(sumaCuadrados / (historicos.size() - 1.0));
        if (desviacion == 0.0) {
            return new AnalisisDTO.Evento(
                    ultimo.fecha(), ultimo.valor(), null, null, historicos.size(),
                    "Los valores históricos del mismo mes no presentan variación.");
        }

        long menores = historicos.stream().filter(valor -> valor < ultimo.valor()).count();
        long iguales = historicos.stream().filter(valor -> valor == ultimo.valor()).count();
        double percentil = 100.0 * (menores + iguales / 2.0) / historicos.size();
        double zScore = (ultimo.valor() - media) / desviacion;
        return new AnalisisDTO.Evento(ultimo.fecha(), ultimo.valor(), zScore, percentil, historicos.size(), null);
    }

    private static LocalDate parsearFecha(String texto, String nombreParametro) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El parámetro '" + nombreParametro + "' es obligatorio");
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "El parámetro '" + nombreParametro + "' debe tener formato YYYY-MM-DD");
        }
    }

    private record RelacionBase(String variableA, String variableB) {}
}
