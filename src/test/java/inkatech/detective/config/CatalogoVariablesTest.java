package inkatech.detective.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogoVariablesTest {

    private final CatalogoVariables catalogo = new CatalogoVariables();

    @Test
    void containsTheFourAgreedVariablesWithCompleteMetadata() {
        assertThat(catalogo.listar())
                .hasSize(4)
                .extracting(VariableConfig::id)
                .containsExactly(
                        "sea-surface-temperature",
                        "precipitation",
                        "eastward-wind",
                        "chlorophyll-a");

        assertThat(catalogo.listar()).allSatisfy(variable -> {
            assertThat(variable.definicion()).isNotBlank();
            assertThat(variable.unidad()).isNotBlank();
            assertThat(variable.dataset()).isNotBlank();
            assertThat(variable.expectativaElNino()).isNotBlank();
            assertThat(variable.archivo()).startsWith("data/raw/");
        });
    }

    @Test
    void findsVariablesByTheirStableIdentifier() {
        assertThat(catalogo.buscar("precipitation"))
                .get()
                .extracting(VariableConfig::nombre)
                .isEqualTo("Precipitacion");
        assertThat(catalogo.buscar("unknown")).isEmpty();
    }
}
