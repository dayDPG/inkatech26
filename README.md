# Detective del Clima

> Nos apoyamos dela IA para generar los Test y lo de Theil-Sen y Mann-Kendall.

Explorar series ambientales del Pacífico y la costa peruana durante episodios asociados a El Niño. Combina una API Spring Boot con una interfaz web estática, gráficos Plotly.js y datos descargados de NASA Giovanni.

> **Alcance:** describe asociaciones y cambios históricos. No es un pronóstico, no estima daños y no reemplaza los comunicados oficiales de ENFEN.


- [Validación externa de estadísticos ](docs/validacion-estadisticos.md)
- [CSV de trabajo y metadatos](src/main/java/inkatech/detective/csv/CSV_DATA.md)
- [Requisitos y ejecución](#requisitos-y-ejecución)


## Funciones

- Explorar temperatura superficial del mar, precipitación, viento zonal y clorofila-a.
- Graficar series y estimar tendencia con pendiente Theil–Sen e inferencia Mann–Kendall.
- Calcular correlación de Spearman con rezagos de 0 a 4 meses para viento -> mar, mar -> lluvia y mar -> clorofila.
- Cargar dos ejemplos guiados: El Niño costero de 2017 y El Niño 2023–24.
- Comparar las versiones Late y Final de precipitación y consultar las analogías históricas implementadas.

## ¿Qué evalúa la aplicación?
 
La aplicación comprueba, con datos de satélite, **si las señales asociadas a El Niño costero se ven en el mar y la costa de Perú, qué tan fuertes son y si se salen de lo normal**. No evalúa si habrá otro El Niño.
 
Un episodio de El Niño tiene una firma conocida: los vientos alisios se debilitan, el mar se calienta, llueve más en el norte de la costa y baja la clorofila. La aplicación revisa, pista por pista, si esa firma aparece en los datos y si se distingue del azar.

## ¿Qué esperar de cada variable en un episodio de El Niño?
 
Son hipótesis para contrastar con los datos, no resultados garantizados.
 
| Variable | Qué mide | Señal esperada | Para tener en cuenta |
|---|---|---|---|
| Temperatura del mar | Temperatura de la capa superior del océano (MODIS-Aqua, versión nocturna) | Anomalía positiva | La región de cada serie está en `CatalogoVariables` |
| Viento zonal | Componente este–oeste del viento a 10 m (MERRA-2, reanálisis) | Alisios más débiles | Los alisios soplan hacia el oeste y salen negativos: un alisio débil se ve como un valor más cercano a cero, es decir, una anomalía positiva |
| Precipitación | Lluvia estimada por satélite (GPM IMERG) | Más lluvia en el norte; efecto menor hacia el sur | Lima es una zona muy seca. La versión Late es preliminar |
| Clorofila-a | Indicador de fitoplancton superficial (MODIS-Aqua) | Menos clorofila por un afloramiento más débil | Termina en noviembre de 2025 y tiene huecos por nubes cerca de la costa |

## Limitaciones
 
- Los datos mensuales del clima no son independientes entre sí (un mes cálido suele ir seguido de otro cálido), por lo que los valores p pueden ser algo optimistas.
- La tendencia observada entre 2002 y 2026 depende del periodo elegido y se ve influida por los eventos fuertes, sobre todo los más recientes. No equivale al calentamiento de fondo del mar.
- La lluvia Late es una estimación preliminar, menos precisa que la versión Final.
- La clorofila no tiene datos de 2026, así que no permite afirmar un efecto en el evento actual; sirve para analogías con eventos pasados.
- El viento de MERRA-2 es un reanálisis (observaciones combinadas con un modelo), no una medición directa.
- Una correlación, incluso significativa, no demuestra causa ni anticipa por sí sola un impacto.
- Hay pocos episodios fuertes en la serie, así que las comparaciones entre eventos son ilustrativas.
## Glosario
 
| Término | Significado |
|---|---|
| Anomalía | Diferencia entre un valor y el promedio de ese mismo mes del año |
| Tendencia | Dirección y ritmo de cambio de una serie durante un periodo |
| Pendiente Theil–Sen | Mediana de las pendientes entre todos los pares de puntos; es poco sensible a valores extremos |
| Mann–Kendall | Prueba estadística que detecta si una serie sube o baja de forma sostenida |
| Intervalo de confianza del 95 % | Rango de valores del cambio que son compatibles con los datos |
| Valor p | Qué tan sorprendente sería el resultado si solo hubiera azar |
| z-score | Distancia a lo habitual, medida en desviaciones típicas |
| Percentil | Fracción de los datos de comparación que quedan por debajo de un valor |
| ρ (Spearman) | Medida de qué tanto suben o bajan juntas dos series, basada en sus posiciones ordenadas |
| Rezago | Número de meses de diferencia al comparar dos series |
| Reanálisis | Estimación de la atmósfera que combina observaciones con un modelo |
| Late y Final | Versiones de la lluvia IMERG: Late sale casi en tiempo real; Final se calibra con pluviómetros |

## Requisitos y ejecución

- Java 21
- Maven Wrapper incluido (`mvnw.cmd`)
- Node.js y npm solo para las pruebas de navegador

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Abre <http://localhost:8080/>. La interfaz y Plotly.js se sirven localmente; no requiere cargar Plotly desde un CDN.

## Pruebas

Las pruebas Java cubren análisis estadísticos, agregación de lluvia, marcadores faltantes, endpoints y recursos estáticos:

```powershell
.\mvnw.cmd test
```

Para las pruebas E2E, instala las dependencias y el navegador una vez:

```powershell
npm install
npx playwright install chromium
```

Ejecuta:

```powershell
npm run test:e2e
```

Si no hay un servidor local disponible, Playwright inicia Spring Boot y lo detiene al terminar. Para probar contra otro servidor, defínelo con `BASE_URL`.

## Datos

Los CSV de trabajo están en [`src/main/java/inkatech/detective/csv/`](src/main/java/inkatech/detective/csv/README.md); ahí se registran las variables y sus metadatos de descarga. La configuración vigente está en `CatalogoVariables`.

| Señal | Producto | Cobertura declarada |
|---|---|---|
| Temperatura del mar | MODIS-Aqua, versión nocturna | 2002-07 a 2026-08 |
| Clorofila-a | MODIS-Aqua | 2002-07 a 2025-11 |
| Lluvia diaria | GPM IMERG v07 Late | 2002-01 a 2026-11 |
| Lluvia mensual de validación | GPM IMERG v07 Final | 2002-01 a 2025-09 |
| Viento zonal | MERRA-2 | 2002-01 a 2026-09 |

Las fechas reflejan los archivos incorporados y pueden cambiar al actualizarlos. En precipitación, los valores diarios se agregan a mm/mes y los datos incompletos se marcan como parciales. Los valores ausentes permanecen como faltantes; no se interpolan.

Las series mensuales se normalizan a un punto por mes; si hay más de una observación completa en un mes, se promedian. La tendencia requiere al menos 24 observaciones mensuales completas.

## API

`GET /api/variables` devuelve el catálogo y los identificadores válidos.

| Ruta | Parámetros | Resultado |
|---|---|---|
| `/api/serie` | `var`, opcionalmente `desde` y `hasta` (`YYYY-MM-DD`) | Serie temporal filtrada |
| `/api/tendencia` | `var`, opcionalmente `desde` y `hasta` | Pendiente Theil–Sen por década, intervalo de confianza y p Mann–Kendall |
| `/api/correlacion` | `a`, `b`, `rezago=0..4` | Spearman, valor p aproximado y pares válidos |
| `/api/evento` | `var` | Comparación de enero–agosto de 2026 con la base anual 2002–2025 |
| `/api/validacion-lluvia` | — | Comparación de lluvia Late agregada y Final en meses compartidos |
| `/api/analogias` | — | Correlación temperatura–clorofila en 2017 y enero de 2023–noviembre de 2024 |

En correlación, el rezago compara `a(t)` con `b(t + rezago)`. Las anomalías se calculan respecto de la climatología mensual de cada serie; los puntos faltantes y parciales no forman pares.

La aplicación describe cambios y asociaciones históricas; no calcula una probabilidad futura de El Niño ni pronostica sus impactos.

## Referencias

- Comisión Multisectorial ENFEN. (s. f.). *Comunicados, avisos y documentos oficiales*. Instituto del Mar del Perú. <https://enfen.imarpe.gob.pe/>. Consultado el 15 de noviembre de 2026.
- Instituto del Mar del Perú. (s. f.). *El Niño – La Niña* (información institucional sobre ENFEN). <https://www.imarpe.gob.pe/nino/>. Consultado el 15 de noviembre de 2026.
- NOAA Climate Prediction Center. (s. f.). *ENSO Diagnostic Discussion*. <https://www.cpc.ncep.noaa.gov/products/analysis_monitoring/enso_advisory/>. Consultado el 15 de noviembre de 2026.
- Royal Netherlands Meteorological Institute (KNMI). (s. f.). *KNMI Climate Explorer*. <https://climexp.knmi.nl/start.cgi>. Consultado el 15 de noviembre de 2026.
- NASA Goddard Earth Sciences Data and Information Services Center. (s. f.). *Giovanni: The Bridge Between Data and Science*. <https://giovanni.gsfc.nasa.gov/giovanni/>. Consultado el 15 de noviembre de 2026.
- McClain, C. R. (2009). A decade of satellite ocean color observations. *Annual Review of Marine Science, 1*, 19–42. <https://doi.org/10.1146/annurev.marine.010908.163650>.
- O'Reilly, J. E., Maritorena, S., Mitchell, B. G., Siegel, D. A., Carder, K. L., Garver, S. A., Kahru, M., & McClain, C. (1998). Ocean color chlorophyll algorithms for SeaWiFS. *Journal of Geophysical Research: Oceans, 103*(C11), 24937–24953. <https://doi.org/10.1029/98JC02160>.

Los artículos de SeaWiFS se incluyen como contexto metodológico sobre color oceánico y clorofila; las series actuales de este proyecto son MODIS-Aqua, no SeaWiFS.

