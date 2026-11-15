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

/**
 * Catalogo de series del proyecto. Cada entrada es una variable en una region,
 * con su propio CSV. Las series de una misma variable comparten "grupo".
 *
 * Lista final de series del proyecto (9). Si se descartan o agregan series, se
 * cambia
 * solo createVariables() y el metodo de cada serie.
 */
@Component
public final class CatalogoVariables {

        /** Carpeta de los CSV. Cambiar aqui si se mueven (ticket T04). */
        private static final String RUTA = "src/main/java/inkatech/detective/csv/";

        private static final String GRUPO_SST = "sea-surface-temperature";
        private static final String GRUPO_CLOROFILA = "chlorophyll-a";
        private static final String GRUPO_LLUVIA = "precipitation";
        private static final String GRUPO_VIENTO = "eastward-wind";

        private static final RegionGeografica COSTA_TOTAL = new RegionGeografica(
                        "Costa peruana (total)", -85.0342, -12.3926, -76.5527, -3.3838);
        private static final RegionGeografica FRANJA_NORTE = new RegionGeografica(
                        "Franja costera norte", -83.5, -8.5, -79.5, -3.4);
        private static final RegionGeografica FRANJA_CENTRO_SUR = new RegionGeografica(
                        "Franja costera centro-sur", -80.5, -12.4, -77.0, -8.5);
        private static final RegionGeografica LLUVIA_NORTE = new RegionGeografica(
                        "Tumbes-Piura", -81.5, -6.5, -79.0, -3.5);
        private static final RegionGeografica LLUVIA_CENTRO = new RegionGeografica(
                        "Lambayeque-La Libertad-Ancash", -80.5, -10.0, -77.5, -6.5);
        private static final RegionGeografica LLUVIA_LIMA = new RegionGeografica(
                        "Lima", -77.3, -12.5, -76.7, -11.5);
        private static final RegionGeografica VIENTO_PACIFICO = new RegionGeografica(
                        "Pacífico ecuatorial", -170.0, -5.0, -120.0, 5.0);

        private static final Periodo PERIODO_SST = new Periodo(
                        LocalDate.of(2002, 7, 4), LocalDate.of(2026, 8, 31));
        private static final Periodo PERIODO_CLOROFILA = new Periodo(
                        LocalDate.of(2002, 7, 4), LocalDate.of(2025, 11, 30));
        private static final Periodo PERIODO_LLUVIA_LATE = new Periodo(
                        LocalDate.of(2002, 1, 1), LocalDate.of(2026, 11, 13));
        private static final Periodo PERIODO_LLUVIA_FINAL = new Periodo(
                        LocalDate.of(2002, 1, 1), LocalDate.of(2025, 9, 30));
        private static final Periodo PERIODO_VIENTO = new Periodo(
                        LocalDate.of(2002, 1, 1), LocalDate.of(2026, 9, 30));

        private final Map<String, VariableConfig> variables;

        public CatalogoVariables() {
                this.variables = createVariables();
        }

        public List<VariableConfig> listar() {
                return List.copyOf(variables.values());
        }

        public List<VariableConfig> obtenerTodas() {
                return listar();
        }

        public List<VariableConfig> listarPorGrupo(String grupo) {
                return variables.values().stream()
                                .filter(variable -> variable.grupo().equals(grupo))
                                .toList();
        }

        public Optional<VariableConfig> buscar(String id) {
                return Optional.ofNullable(variables.get(id));
        }

        public Optional<VariableConfig> obtenerPorId(String id) {
                return buscar(id);
        }

        private static Map<String, VariableConfig> createVariables() {
                Map<String, VariableConfig> result = new LinkedHashMap<>();
                add(result, temperaturaMarTotal());
                add(result, clorofilaTotal());
                add(result, clorofilaFranjaNorte());
                add(result, clorofilaFranjaCentroSur());
                add(result, lluviaNorte());
                add(result, lluviaCentro());
                add(result, lluviaLima());
                add(result, lluviaNorteFinal());
                add(result, vientoPacífico());
                return Collections.unmodifiableMap(result);
        }

        private static void add(Map<String, VariableConfig> variables, VariableConfig variable) {
                variables.put(variable.id(), variable);
        }

        private static VariableConfig temperaturaMarTotal() {
                return new VariableConfig(
                                "sea-surface-temperature-total",
                                "Temperatura superficial del mar",
                                "Temperatura de la capa superior del oceano observada por MODIS-Aqua.",
                                "°C",
                                COSTA_TOTAL,
                                "MODISA_L3m_NSST_Monthly_4km vR2019.0",
                                "Durante El Nino se esperan temperaturas mayores de lo habitual.",
                                RUTA + "temp_mar.csv",
                                GRUPO_SST,
                                Resolucion.MENSUAL,
                                PERIODO_SST,
                                "Se usa solo la version nocturna; no mezclar con la diurna.");
        }

        private static VariableConfig clorofilaTotal() {
                return new VariableConfig(
                                "chlorophyll-a-total",
                                "Clorofila-a",
                                "Concentracion del pigmento usado como indicador de fitoplancton superficial.",
                                "mg/m³",
                                COSTA_TOTAL,
                                "MODISA_L3m_CHL v2022.0",
                                "El calentamiento y menor afloramiento pueden reducir su concentracion.",
                                RUTA + "clorofila_total.csv",
                                GRUPO_CLOROFILA,
                                Resolucion.MENSUAL,
                                PERIODO_CLOROFILA,
                                "Los datos terminan en noviembre de 2025 (sin 2026). Cerca de la costa hay huecos por nubes.");
        }

        private static VariableConfig clorofilaFranjaNorte() {
                return new VariableConfig(
                                "chlorophyll-a-franja-norte",
                                "Clorofila-a (franja costera norte)",
                                "Concentracion del pigmento usado como indicador de fitoplancton superficial, cerca de la costa.",
                                "mg/m³",
                                FRANJA_NORTE,
                                "MODISA_L3m_CHL v2022.0",
                                "El afloramiento costero se debilita con El Nino, asi que se espera menos clorofila.",
                                RUTA + "clorofila_norte.csv",
                                GRUPO_CLOROFILA,
                                Resolucion.MENSUAL,
                                PERIODO_CLOROFILA,
                                "Los datos terminan en noviembre de 2025 (sin 2026). Cerca de la costa hay huecos por nubes.");
        }

        private static VariableConfig clorofilaFranjaCentroSur() {
                return new VariableConfig(
                                "chlorophyll-a-franja-centro-sur",
                                "Clorofila-a (franja costera centro-sur)",
                                "Concentracion del pigmento usado como indicador de fitoplancton superficial, cerca de la costa.",
                                "mg/m³",
                                FRANJA_CENTRO_SUR,
                                "MODISA_L3m_CHL v2022.0",
                                "El afloramiento costero se debilita con El Nino, asi que se espera menos clorofila.",
                                RUTA + "clorofila_centrosur.csv",
                                GRUPO_CLOROFILA,
                                Resolucion.MENSUAL,
                                PERIODO_CLOROFILA,
                                "Los datos terminan en noviembre de 2025 (sin 2026). Cerca de la costa hay huecos por nubes.");
        }

        private static VariableConfig lluviaNorte() {
                return new VariableConfig(
                                "precipitation-norte",
                                "Precipitacion (Tumbes-Piura)",
                                "Lluvia diaria estimada por satelite (IMERG, version Late).",
                                "mm/dia",
                                LLUVIA_NORTE,
                                "GPM_3IMERGDL v07",
                                "Un evento costero puede favorecer lluvias por encima de lo habitual, sobre todo en el norte.",
                                RUTA + "lluvia_norte.csv",
                                GRUPO_LLUVIA,
                                Resolucion.DIARIA,
                                PERIODO_LLUVIA_LATE,
                                "Dato preliminar: la version Late no esta calibrada con pluviometros.");
        }

        private static VariableConfig lluviaCentro() {
                return new VariableConfig(
                                "precipitation-centro",
                                "Precipitacion (Lambayeque-La Libertad-Ancash)",
                                "Lluvia diaria estimada por satelite (IMERG, version Late).",
                                "mm/dia",
                                LLUVIA_CENTRO,
                                "GPM_3IMERGDL v07",
                                "Hipotesis: el efecto sobre la lluvia es menor que en el norte.",
                                RUTA + "lluvia_centronorte.csv",
                                GRUPO_LLUVIA,
                                Resolucion.DIARIA,
                                PERIODO_LLUVIA_LATE,
                                "Dato preliminar: la version Late no esta calibrada con pluviometros.");
        }

        private static VariableConfig lluviaLima() {
                return new VariableConfig(
                                "precipitation-lima",
                                "Precipitacion (Lima)",
                                "Lluvia diaria estimada por satelite (IMERG, version Late).",
                                "mm/dia",
                                LLUVIA_LIMA,
                                "GPM_3IMERGDL v07",
                                "Hipotesis: Lima es muy seca y se espera poco cambio frente al norte.",
                                RUTA + "lluvia_lima.csv",
                                GRUPO_LLUVIA,
                                Resolucion.DIARIA,
                                PERIODO_LLUVIA_LATE,
                                "Dato preliminar: la version Late no esta calibrada con pluviometros.");
        }

        private static VariableConfig lluviaNorteFinal() {
                return new VariableConfig(
                                "precipitation-norte-final",
                                "Precipitacion (Tumbes-Piura, version final)",
                                "Lluvia mensual estimada combinando satelites y pluviometros (IMERG, version Final).",
                                "mm/hr",
                                LLUVIA_NORTE,
                                "GPM_3IMERGM v07",
                                "Se usa para validar la serie Late; no para el evento de 2026.",
                                RUTA + "lluvia_norteValidacion.csv",
                                GRUPO_LLUVIA,
                                Resolucion.MENSUAL,
                                PERIODO_LLUVIA_FINAL,
                                "La unidad es la que se descargo de Giovanni (mm/hr por defecto): convertir antes de comparar con la Late (ticket T15).");
        }

        private static VariableConfig vientoPacífico() {
                return new VariableConfig(
                                "eastward-wind-pacifico",
                                "Viento zonal del Pacífico",
                                "Componente este-oeste del viento a diez metros sobre la superficie en el Pacífico ecuatorial.",
                                "m/s",
                                VIENTO_PACIFICO,
                                "M2TMNXSLV v5.12.4",
                                "Se usa para analizar las condiciones del Pacífico ecuatorial durante El Nino.",
                                RUTA + "viento_pacifico.csv",
                                GRUPO_VIENTO,
                                Resolucion.MENSUAL,
                                PERIODO_VIENTO,
                                "Los alisios soplan hacia el oeste y salen negativos: un alisio debil se ve como un valor mas cercano a cero (anomalia positiva). Dato de reanalisis (MERRA-2), no medicion directa. Este recuadro es del Pacífico ecuatorial: refleja el viento regional, no el de toda la zona.");
        }
}