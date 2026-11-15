package inkatech.detective.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarVariablesDevuelveFichasConFechas() throws Exception {
        mockMvc.perform(get("/api/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
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
                .andExpect(jsonPath("$.pendientePorDecada").exists())
                .andExpect(jsonPath("$.valorP").exists())
                .andExpect(jsonPath("$.intervaloConfianzaInferior").exists())
                .andExpect(jsonPath("$.intervaloConfianzaSuperior").exists());
    }

    @Test
    void calculaTendenciaDePrecipitacionLuegoDeAgregarPorMes() throws Exception {
        mockMvc.perform(get("/api/tendencia").param("var", "precipitation-norte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendientePorDecada").exists());
    }

    @Test
    void variableDesconocidaDevuelve404() throws Exception {
        mockMvc.perform(get("/api/serie").param("var", "desconocida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Variable no encontrada: desconocida"));
    }

    @Test
    void calculaCorrelacionSpearmanConRezago() throws Exception {
        mockMvc.perform(get("/api/correlacion")
                        .param("a", VARIABLE_VIENTO)
                        .param("b", VARIABLE_TEMPERATURA)
                        .param("rezago", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coeficiente").isNumber())
                .andExpect(jsonPath("$.valorP").isNumber())
                .andExpect(jsonPath("$.rezagoMeses").value(2))
                .andExpect(jsonPath("$.numeroPares", greaterThan(3)));
    }

    @Test
    void rechazaCorrelacionConRezagoFueraDelRango() throws Exception {
        mockMvc.perform(get("/api/correlacion")
                        .param("a", VARIABLE_VIENTO)
                        .param("b", VARIABLE_TEMPERATURA)
                        .param("rezago", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El rezago debe estar entre 0 y 4 meses"));
    }
}
