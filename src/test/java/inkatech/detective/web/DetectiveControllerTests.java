package inkatech.detective.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DetectiveControllerTests {

    private static final String VARIABLE_TEMPERATURA = "sea-surface-temperature-total";
    private static final String VARIABLE_VIENTO = "eastward-wind-pacifico";
    private static final String CLOROFILA_TOTAL = "chlorophyll-a-total";
    private static final String LLUVIA_DIARIA_NORTE = "precipitation-norte";
    private static final String LLUVIA_MENSUAL_FINAL = "precipitation-norte-final";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarVariablesDevuelveFichasConFechas() throws Exception {
        mockMvc.perform(get("/api/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].periodo.desde").exists())
                .andExpect(jsonPath("$[0].periodo.hasta").exists());
    }

    @Test
    void obtenerSerieFiltraLosPuntosPorRango() throws Exception {
        mockMvc.perform(get("/api/serie")
                        .param("var", VARIABLE_TEMPERATURA)
                        .param("desde", "2002-08-01")
                        .param("hasta", "2002-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variableId").value(VARIABLE_TEMPERATURA))
                .andExpect(jsonPath("$.unidad").value("°C"))
                .andExpect(jsonPath("$.puntos", hasSize(3)))
                .andExpect(jsonPath("$.puntos[0].fecha").value("2002-08-01"))
                .andExpect(jsonPath("$.puntos[2].fecha").value("2002-10-01"));
    }

    @Test
    void analisisConsolidaSerieAnomaliasTendenciaEventoYRelaciones() throws Exception {
        mockMvc.perform(get("/api/analisis")
                        .param("var", VARIABLE_TEMPERATURA)
                        .param("desde", "2019-01-01")
                        .param("hasta", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serie.variableId").value(VARIABLE_TEMPERATURA))
                .andExpect(jsonPath("$.serie.puntos", hasSize(greaterThan(24))))
                .andExpect(jsonPath("$.anomalias.puntos", hasSize(greaterThan(24))))
                .andExpect(jsonPath("$.tendencia.numeroDePuntos", greaterThan(24)))
                .andExpect(jsonPath("$.evento.fecha").exists())
                .andExpect(jsonPath("$.evento.zScore").isNumber())
                .andExpect(jsonPath("$.evento.percentil").isNumber())
                .andExpect(jsonPath("$.relaciones", hasSize(3)))
                .andExpect(jsonPath("$.relaciones[0].rezagos", hasSize(5)))
                .andExpect(jsonPath("$.relaciones[0].rezagos[4].numeroDePares", greaterThan(0)));
    }

    @Test
    void analisisDevuelveTendenciaNulaConMensajeParaPeriodoCorto() throws Exception {
        mockMvc.perform(get("/api/analisis")
                        .param("var", VARIABLE_TEMPERATURA)
                        .param("desde", "2024-01-01")
                        .param("hasta", "2024-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tendencia").value(nullValue()))
                .andExpect(jsonPath("$.mensajeTendencia").isNotEmpty())
                .andExpect(jsonPath("$.relaciones[0].rezagos", hasSize(5)));
    }

    @Test
    void analisisValidaParametrosConRespuestaDeMensaje() throws Exception {
        mockMvc.perform(get("/api/analisis")
                        .param("var", VARIABLE_TEMPERATURA)
                        .param("desde", "ayer")
                        .param("hasta", "2024-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void obtieneSerieDeClorofilaConValoresYUnidad() throws Exception {
        mockMvc.perform(get("/api/serie")
                        .param("var", CLOROFILA_TOTAL)
                        .param("desde", "2023-01-01")
                        .param("hasta", "2023-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variableId").value(CLOROFILA_TOTAL))
                .andExpect(jsonPath("$.unidad").value("mg/m³"))
                .andExpect(jsonPath("$.puntos", hasSize(3)))
                .andExpect(jsonPath("$.puntos[0].valor").isNumber());
    }

    @Test
    void obtenerTendenciaDevuelveResultadoEstadistico() throws Exception {
        mockMvc.perform(get("/api/tendencia")
                        .param("var", VARIABLE_VIENTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDePuntos", greaterThan(0)))
                .andExpect(jsonPath("$.pendientePorDecada").exists())
                .andExpect(jsonPath("$.valorP").exists())
                .andExpect(jsonPath("$.intervaloConfianzaInferior").exists())
                .andExpect(jsonPath("$.intervaloConfianzaSuperior").exists());
    }

    @Test
    void obtieneTendenciaDeTemperaturaAunqueHayaObservacionesDuplicadasEnUnMes() throws Exception {
        mockMvc.perform(get("/api/tendencia")
                        .param("var", VARIABLE_TEMPERATURA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDePuntos", greaterThan(24)))
                .andExpect(jsonPath("$.pendientePorDecada").exists())
                .andExpect(jsonPath("$.valorP").exists());
    }

    @Test
    void correlacionPermiteVientoHaciaMarConRezagoYValorP() throws Exception {
        mockMvc.perform(get("/api/correlacion")
                        .param("a", VARIABLE_VIENTO)
                        .param("b", VARIABLE_TEMPERATURA)
                        .param("rezago", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coeficiente").exists())
                .andExpect(jsonPath("$.rezagoMeses").value(2))
                .andExpect(jsonPath("$.numeroPares", greaterThan(0)))
                .andExpect(jsonPath("$.valorP").exists());
    }

    @Test
    void correlacionPermiteTemperaturaHaciaClorofilaNorte() throws Exception {
        mockMvc.perform(get("/api/correlacion")
                        .param("a", VARIABLE_TEMPERATURA)
                        .param("b", "chlorophyll-a-franja-norte")
                        .param("rezago", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroPares", greaterThan(0)))
                .andExpect(jsonPath("$.valorP").exists());
    }

    @Test
    void correlacionRechazaParesNoPermitidos() throws Exception {
        mockMvc.perform(get("/api/correlacion")
                        .param("a", CLOROFILA_TOTAL)
                        .param("b", VARIABLE_VIENTO)
                        .param("rezago", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventoActualComparaTemperaturaHastaAgostoDe2026ConLaBase() throws Exception {
        mockMvc.perform(get("/api/evento")
                        .param("var", VARIABLE_TEMPERATURA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodoBaseDesde").value(2003))
                .andExpect(jsonPath("$.periodoBaseHasta").value(2025))
                .andExpect(jsonPath("$.mesesValidos2026").value(8))
                .andExpect(jsonPath("$.zScore").exists())
                .andExpect(jsonPath("$.percentil").exists())
                .andExpect(jsonPath("$.periodoRetornoAnios").exists());
    }

    @Test
    void validacionDeLluviaComparaMesesCompartidosDeLateYFinal() throws Exception {
        mockMvc.perform(get("/api/validacion-lluvia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desde").value("2002-08-01"))
                .andExpect(jsonPath("$.hasta").value("2025-09-30"))
                .andExpect(jsonPath("$.metodoCorrelacion").value("Spearman"))
                .andExpect(jsonPath("$.numeroDeMeses", greaterThan(0)))
                .andExpect(jsonPath("$.correlacion").exists())
                .andExpect(jsonPath("$.sesgoMedioLateMenosFinal").exists())
                .andExpect(jsonPath("$.unidad").value("mm/mes"));
    }

    @Test
    void analogiasComparaTemperaturaYClorofilaEnLosDosEventos() throws Exception {
        mockMvc.perform(get("/api/analogias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variableTemperatura").value(VARIABLE_TEMPERATURA))
                .andExpect(jsonPath("$.variableClorofila").value(CLOROFILA_TOTAL))
                .andExpect(jsonPath("$.corteDatosHasta").value("2025-11-30"))
                .andExpect(jsonPath("$.eventos", hasSize(2)))
                .andExpect(jsonPath("$.eventos[0].nombre").value("2017"))
                .andExpect(jsonPath("$.eventos[0].numeroDePares", greaterThan(0)))
                .andExpect(jsonPath("$.eventos[1].nombre").value("2023-2024"))
                .andExpect(jsonPath("$.eventos[1].rhoSpearman").exists());
    }

    @Test
    void paginaPrincipalCargaLosAssetsQueExistenEnStatic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/tokens.css")))
                .andExpect(content().string(containsString("/style.css")))
                .andExpect(content().string(containsString("/js/plotly.min.js")))
                .andExpect(content().string(containsString("/javascript.js")));

        mockMvc.perform(get("/tokens.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/style.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/javascript.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/js/plotly.min.js"))
                .andExpect(status().isOk());
    }

    @Test
    void serieDeLluviaDiariaSeDevuelveAcumuladaYParcialEnNoviembreDe2026() throws Exception {
        mockMvc.perform(get("/api/serie")
                        .param("var", LLUVIA_DIARIA_NORTE)
                        .param("desde", "2026-11-01")
                        .param("hasta", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidad").value("mm/mes"))
                .andExpect(jsonPath("$.puntos", hasSize(1)))
                .andExpect(jsonPath("$.puntos[0].fecha").value("2026-11-01"))
                .andExpect(jsonPath("$.puntos[0].parcial").value(true));
    }

    @Test
    void serieFinalEnMilimetrosPorHoraSeConvierteAporteMensual() throws Exception {
        mockMvc.perform(get("/api/serie")
                        .param("var", LLUVIA_MENSUAL_FINAL)
                        .param("desde", "2002-01-01")
                        .param("hasta", "2002-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidad").value("mm/mes"))
                .andExpect(jsonPath("$.puntos[0].valor")
                        .value(closeTo(0.0302429944 * 31 * 24, 1.0e-8)));
    }
}
