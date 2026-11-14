# Epica 0: preparacion y adquisicion de datos

Este documento fija las decisiones de T02 y los criterios de cierre de T03-T06.
Las credenciales de Earthdata son personales y no deben guardarse en este repositorio.

## Preparacion de Earthdata (T02)

Cada integrante debe:

1. Crear su perfil en [Earthdata Login](https://urs.earthdata.nasa.gov/users/new).
2. Iniciar sesion en [Giovanni](https://giovanni.gsfc.nasa.gov/giovanni/).
3. Autorizar la aplicacion `NASA GESDISC DATA ARCHIVE` cuando Earthdata lo solicite.
4. Confirmar que puede generar y descargar una serie temporal en CSV.

Giovanni recibe los recuadros en el orden `oeste, sur, este, norte`. Las longitudes
oeste y las latitudes sur se expresan con numeros negativos.

## Recuadros finales

| Variable | Region | Oeste | Sur | Este | Norte | Justificacion |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Temperatura superficial del mar | Nino 1+2 | -90.0 | -10.0 | -80.0 | 0.0 | Definicion geografica de Nino 1+2 usada por NOAA. |
| Precipitacion | Piura-Tumbes | -82.5 | -6.5 | -79.0 | -3.3 | Cubre ambos departamentos y su franja costera. |
| Viento zonal superficial | Pacifico ecuatorial central-oriental | -170.0 | -5.0 | -80.0 | 5.0 | Banda ecuatorial sin cruzar el antimeridiano, hasta la costa sudamericana. |
| Clorofila-a | Costa peruana | -84.0 | -18.5 | -70.0 | 0.0 | Cubre el mar adyacente a toda la costa del Peru; las celdas terrestres no aportan valores. |

Estos recuadros son decisiones del proyecto. Cualquier cambio posterior debe quedar
registrado, porque modifica las series y puede cambiar los resultados estadisticos.

## Configuracion comun de descarga

- Visualizacion: `Area-Averaged Time Series`.
- Frecuencia: mensual.
- Periodo comun inicial: enero de 2003 hasta el ultimo mes completo disponible.
- Formato: CSV descargado desde la seccion `Downloads` del resultado.
- No editar manualmente las lineas de metadatos de Giovanni.
- Guardar el archivo con el nombre indicado en la tabla siguiente.

## Archivos requeridos (T03-T06)

| Ticket | Archivo | Producto/variable a seleccionar en Giovanni |
| --- | --- | --- |
| T03 | `data/raw/sea_surface_temperature_nino12.csv` | `Sea Surface Temperature at 11 microns (Night) (water only)`, `MODISA_L3m_NSST_Monthly_4km`, vR2019.0 |
| T04 | `data/raw/precipitation_piura_tumbes.csv` | `Merged satellite-gauge precipitation estimate - Final Run (recommended for general use)`, `GPM_3IMERGM`, v07 |
| T05 | `data/raw/eastward_wind_equatorial_pacific.csv` | `10-meter eastward wind`, `M2TMNXSLV`, v5.12.4 |
| T06 | `data/raw/chlorophyll_a_peruvian_coast.csv` | `Chlorophyll a concentration (water only)`, `MODISA_L3m_CHL`, v2022.0 |

## Criterios de aceptacion

Un archivo se acepta cuando:

- existe con el nombre acordado y no esta vacio;
- conserva los metadatos originales de Giovanni;
- contiene fechas mensuales y una columna numerica de valores;
- el recuadro y el producto de sus metadatos coinciden con este documento;
- cubre desde enero de 2003 hasta el ultimo mes completo disponible para el producto;
- los faltantes permanecen identificables y no fueron reemplazados manualmente por cero.

T02 se cierra cuando ambos integrantes confirman acceso y descarga. T03-T06 se cierran
solo despues de que los cuatro CSV superen estos criterios.
