package inkatech.detective.config;

import inkatech.detective.config.VariableConfig.Periodo;
import inkatech.detective.config.VariableConfig.Resolucion;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class CatalogoVariables {
    private static final String RUTA = "data/raw/";
    private final Map<String, VariableConfig> variables = crearVariables();

    public List<VariableConfig> listar() { return List.copyOf(variables.values()); }
    public Optional<VariableConfig> buscar(String id) { return Optional.ofNullable(variables.get(id)); }

    private static Map<String, VariableConfig> crearVariables() {
        Map<String, VariableConfig> r = new LinkedHashMap<>();
        add(r, v("sea-surface-temperature-total", "Temperatura superficial del mar",
                "Temperatura de la capa superior del oceano observada por MODIS-Aqua.", "°C",
                region("Costa peruana", -85.0342, -12.3926, -76.5527, -3.3838),
                "MODISA_L3m_SST4_Monthly_4km vR2019.0", "Se esperan temperaturas mayores de lo habitual.",
                "temp_mar.csv", "sea-surface-temperature", Resolucion.MENSUAL, d(2002, 8, 6), d(2026, 8, 1), "Serie nocturna SST4."));
        add(r, chlorophyll("chlorophyll-a-total", "Clorofila-a", "Costa peruana",
                -85.0342, -12.3926, -76.5527, -3.3838, "clorofila_total.csv"));
        add(r, chlorophyll("chlorophyll-a-norte", "Clorofila-a (norte)", "Franja costera norte",
                -83.5, -8.5, -79.5, -3.4, "clorofila_norte.csv"));
        add(r, chlorophyll("chlorophyll-a-centro-sur", "Clorofila-a (centro-sur)", "Franja costera centro-sur",
                -80.5, -12.4, -77.0, -8.5, "clorofila_centrosur.csv"));
        add(r, rain("precipitation-norte", "Precipitacion (Tumbes-Piura)", "Tumbes-Piura",
                -81.5, -6.5, -79.0, -3.5, "lluvia_norte.csv"));
        add(r, rain("precipitation-centro", "Precipitacion (centro-norte)", "Lambayeque-La Libertad-Ancash",
                -80.5, -10.0, -77.5, -6.5, "lluvia_centronorte.csv"));
        add(r, rain("precipitation-lima", "Precipitacion (Lima)", "Lima",
                -77.3, -12.5, -76.7, -11.5, "lluvia_lima.csv"));
        add(r, v("precipitation-norte-final", "Precipitacion (norte, version final)",
                "Precipitacion mensual IMERG combinada con pluviometros.", "mm/hr",
                region("Tumbes-Piura", -81.5, -6.5, -79.0, -3.5), "GPM_3IMERGM v07",
                "Sirve para validar la serie Late.", "lluvia_norte_validacion.csv", "precipitation",
                Resolucion.MENSUAL, d(2002, 1, 1), d(2025, 9, 1), "Convertir unidades antes de comparar."));
        add(r, v("eastward-wind-pacifico", "Viento zonal superficial",
                "Componente este-oeste del viento a diez metros.", "m/s",
                region("Pacifico ecuatorial", -170, -5, -120, 5), "M2TMNXSLV v5.12.4",
                "Los vientos alisios suelen debilitarse.", "viento_pacifico.csv", "eastward-wind",
                Resolucion.MENSUAL, d(2002, 1, 1), d(2026, 9, 1), "Positivo indica viento hacia el este."));
        return Collections.unmodifiableMap(r);
    }

    private static VariableConfig chlorophyll(String id, String name, String regionName,
            double west, double south, double east, double north, String file) {
        return v(id, name, "Concentracion de clorofila-a como indicador de fitoplancton.", "mg/m³",
                region(regionName, west, south, east, north), "MODISA_L3m_CHL v2022.0",
                "Puede reducirse si se debilita el afloramiento.", file, "chlorophyll-a",
                Resolucion.MENSUAL, d(2002, 7, 4), d(2025, 11, 1), "Puede contener huecos por nubosidad.");
    }

    private static VariableConfig rain(String id, String name, String regionName,
            double west, double south, double east, double north, String file) {
        return v(id, name, "Precipitacion diaria estimada por satelite IMERG Late.", "mm/dia",
                region(regionName, west, south, east, north), "GPM_3IMERGDL v07",
                "El Nino costero puede favorecer lluvias por encima de lo habitual.", file, "precipitation",
                Resolucion.DIARIA, d(2002, 1, 1), d(2026, 11, 13), "Dato preliminar sin pluviometros.");
    }

    private static VariableConfig v(String id, String name, String definition, String unit,
            RegionGeografica region, String dataset, String expectation, String file, String group,
            Resolucion resolution, LocalDate from, LocalDate to, String note) {
        return new VariableConfig(id, name, definition, unit, region, dataset, expectation,
                RUTA + file, group, resolution, new Periodo(from, to), note);
    }

    private static RegionGeografica region(String name, double west, double south, double east, double north) {
        return new RegionGeografica(name, west, south, east, north);
    }
    private static LocalDate d(int y, int m, int d) { return LocalDate.of(y, m, d); }
    private static void add(Map<String, VariableConfig> variables, VariableConfig variable) {
        if (variables.put(variable.id(), variable) != null) throw new IllegalStateException("Variable duplicada");
    }
}
