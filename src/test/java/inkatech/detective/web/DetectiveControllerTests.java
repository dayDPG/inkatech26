package inkatech.detective.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.puntos[0].fecha").value("2002-08-06"))
                .andExpect(jsonPath("$.puntos[2].fecha").value("2002-09-30"));
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
