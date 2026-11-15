package inkatech.detective.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogoVariablesTest {

    private final CatalogoVariables catalogo = new CatalogoVariables();

    @Test
    void containsTheNineAvailableSeriesWithCompleteMetadata() {
        assertThat(catalogo.listar())
                .hasSize(9)
                .extracting(VariableConfig::id)
                .containsExactly(
                        "sea-surface-temperature-total",
                        "chlorophyll-a-total",
                        "chlorophyll-a-norte",
                        "chlorophyll-a-centro-sur",
                        "precipitation-norte",
                        "precipitation-centro",
                        "precipitation-lima",
                        "precipitation-norte-final",
                        "eastward-wind-pacifico");

        assertThat(catalogo.listar()).allSatisfy(variable -> {
            assertThat(variable.definicion()).isNotBlank();
            assertThat(variable.unidad()).isNotBlank();
            assertThat(variable.dataset()).isNotBlank();
            assertThat(variable.expectativaElNino()).isNotBlank();
            assertThat(variable.archivo()).startsWith("data/raw/");
            assertThat(getClass().getClassLoader().getResource(variable.archivo())).isNotNull();
        });
    }

    @Test
    void findsVariablesByTheirStableIdentifier() {
        assertThat(catalogo.buscar("precipitation-norte"))
                .get()
                .extracting(VariableConfig::nombre)
                .isEqualTo("Precipitacion (Tumbes-Piura)");
        assertThat(catalogo.buscar("unknown")).isEmpty();
    }
}
