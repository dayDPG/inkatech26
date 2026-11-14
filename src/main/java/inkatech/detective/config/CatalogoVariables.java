package inkatech.detective.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class CatalogoVariables {

    private final Map<String, VariableConfig> variables;

    public CatalogoVariables() {
        this.variables = createVariables();
    }

    public List<VariableConfig> listar() {
        return List.copyOf(variables.values());
    }

    public Optional<VariableConfig> buscar(String id) {
        return Optional.ofNullable(variables.get(id));
    }

    private static Map<String, VariableConfig> createVariables() {
        Map<String, VariableConfig> result = new LinkedHashMap<>();
        add(result, seaSurfaceTemperature());
        add(result, precipitation());
        add(result, eastwardWind());
        add(result, chlorophyll());
        return Collections.unmodifiableMap(result);
    }

    private static void add(Map<String, VariableConfig> variables, VariableConfig variable) {
        variables.put(variable.id(), variable);
    }

    private static VariableConfig seaSurfaceTemperature() {
        return new VariableConfig(
                "sea-surface-temperature",
                "Temperatura superficial del mar",
                "Temperatura de la capa superior del oceano observada por MODIS-Aqua.",
                "°C",
                new RegionGeografica("Nino 1+2", -90.0, -10.0, -80.0, 0.0),
                "MODISA_L3m_NSST_Monthly_4km vR2019.0",
                "Durante El Nino se esperan temperaturas mayores de lo habitual.",
                "data/raw/sea_surface_temperature_nino12.csv");
    }

    private static VariableConfig precipitation() {
        return new VariableConfig(
                "precipitation",
                "Precipitacion",
                "Cantidad mensual de lluvia estimada combinando satelites y pluviometros.",
                "mm/mes",
                new RegionGeografica("Piura-Tumbes", -82.5, -6.5, -79.0, -3.3),
                "GPM_3IMERGM v07",
                "Un evento costero puede favorecer lluvias por encima de lo habitual.",
                "data/raw/precipitation_piura_tumbes.csv");
    }

    private static VariableConfig eastwardWind() {
        return new VariableConfig(
                "eastward-wind",
                "Viento zonal superficial",
                "Componente este-oeste del viento a diez metros sobre la superficie.",
                "m/s",
                new RegionGeografica(
                        "Pacifico ecuatorial central-oriental",
                        -170.0,
                        -5.0,
                        -80.0,
                        5.0),
                "M2TMNXSLV v5.12.4",
                "Durante El Nino los vientos alisios suelen debilitarse.",
                "data/raw/eastward_wind_equatorial_pacific.csv");
    }

    private static VariableConfig chlorophyll() {
        return new VariableConfig(
                "chlorophyll-a",
                "Clorofila-a",
                "Concentracion del pigmento usado como indicador de fitoplancton superficial.",
                "mg/m³",
                new RegionGeografica("Costa peruana", -84.0, -18.5, -70.0, 0.0),
                "MODISA_L3m_CHL v2022.0",
                "El calentamiento y menor afloramiento pueden reducir su concentracion.",
                "data/raw/chlorophyll_a_peruvian_coast.csv");
    }
}
