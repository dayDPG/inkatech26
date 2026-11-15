# Datos y metadatos

Las series de trabajo se descargaron de NASA Giovanni. Las regiones se expresan como
`longitud oeste, latitud sur, longitud este, latitud norte`, en grados decimales.
Los productos, archivos y periodos vigentes para la aplicación se configuran en
[`CatalogoVariables`](../src/main/java/inkatech/detective/config/CatalogoVariables.java).

| Variable | Producto | Región | Unidad | Desde | Hasta |
|---|---|---|---|---|---|
| Clorofila-a (total) | MODIS-Aqua `MODISA_L3m_CHL` | `-85.0342,-12.3926,-76.5527,-3.3838` | mg/m³ | 2002-07-04 | 2025-11-30 |
| Clorofila-a (franja norte) | MODIS-Aqua `MODISA_L3m_CHL` | `-83.5,-8.5,-79.5,-3.4` | mg/m³ | 2002-07-04 | 2025-11-30 |
| Clorofila-a (franja centro-sur) | MODIS-Aqua `MODISA_L3m_CHL` | `-80.5,-12.4,-77,-8.5` | mg/m³ | 2002-07-04 | 2025-11-30 |
| Temperatura superficial del mar | MODIS-Aqua nocturna `MODISA_L3m_NSST_Monthly_4km` | `-85.0342,-12.3926,-76.5527,-3.3838` | °C | 2002-07-04 | 2026-08-31 |
| Precipitación diaria (Tumbes-Piura) | GPM IMERG v07 Late `GPM_3IMERGDL` | `-81.5,-6.5,-79,-3.5` | mm/día | 2002-01-01 | 2026-11-13 |
| Precipitación diaria (Lambayeque-Ancash) | GPM IMERG v07 Late `GPM_3IMERGDL` | `-80.5,-10,-77.5,-6.5` | mm/día | 2002-01-01 | 2026-11-13 |
| Precipitación diaria (Lima) | GPM IMERG v07 Late `GPM_3IMERGDL` | `-77.3,-12.5,-76.7,-11.5` | mm/día | 2002-01-01 | 2026-11-13 |
| Precipitación mensual de validación (Tumbes-Piura) | GPM IMERG v07 Final `GPM_3IMERGM` | `-81.5,-6.5,-79,-3.5` | mm/hr | 2002-01-01 | 2025-09-30 |
| Viento zonal a 10 m | MERRA-2 `M2TMNXSLV` v5.12.4 | `-170,-5,-120,5` | m/s | 2002-01-01 | 2026-09-30 |

Las fechas indican la cobertura declarada para los archivos incorporados y pueden
cambiar al actualizarlos. La precipitación diaria se agrega a mm/mes para el análisis;
los meses incompletos se marcan como parciales. Los datos ausentes se conservan como
faltantes y no se interpolan. La serie mensual de precipitación Final se usa para la
validación con la serie Late.

Las series mensuales se normalizan a un punto por mes; cuando hay más de una observación
completa en un mes, se promedian. La tendencia requiere al menos 24 observaciones
mensuales completas. La clorofila termina en noviembre de 2025 y presenta vacíos cerca
de la costa; MERRA-2 es un reanálisis, no una medición directa.
