package inkatech.detective.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import inkatech.detective.config.CatalogoVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class VariableControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VariableController(new CatalogoVariables()))
                .build();
    }

    @Test
    void listsTheFourVariableCards() throws Exception {
        mockMvc.perform(get("/api/variables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].region.nombre").isNotEmpty())
                .andExpect(jsonPath("$[0].dataset").isNotEmpty())
                .andExpect(jsonPath("$[0].expectativaElNino").isNotEmpty());
    }
}
