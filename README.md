# Climate Detective

Climate Detective is an interactive web application for exploring environmental signals associated with El Niño along the Peruvian coast and the equatorial Pacific. It combines a Spring Boot API, an accessible Plotly.js interface, and reproducible statistical methods over satellite and atmospheric reanalysis time series.

> **Scientific scope:** this project describes historical changes and associations. It is not a forecast, does not estimate damage, and does not replace official ENFEN guidance.

## What the application does

- Explores sea-surface temperature, precipitation, eastward wind, and chlorophyll-a by region and period.
- Plots the observed series, monthly anomalies, and a robust trend line.
- Reports Theil–Sen change per decade, a 95% confidence interval, and Mann–Kendall significance.
- Measures lagged Spearman associations from 0 to 4 months for wind → ocean, ocean → rain, and ocean → chlorophyll.
- Compares the 2026 period with the 2002–2025 baseline using z-scores, percentiles, and empirical return periods.
- Includes guided presets for the 2017 Coastal El Niño, the 2023–24 event, and the 2026 period.
- Compares GPM IMERG Late and Final precipitation products and exposes historical analogies.

The interface remains in Spanish for its intended public audience; this project documentation is maintained in English.

## Scientific question

The project asks whether signals commonly associated with a Coastal El Niño appear in the available ocean and coastal records, how large their changes are, and how the variables move together.

| Variable | What it represents | Expected El Niño signal | Important caveat |
| --- | --- | --- | --- |
| Sea-surface temperature | Upper-ocean temperature from MODIS-Aqua | Positive temperature anomaly | Results depend on the selected region and period |
| Eastward wind | East–west wind component at 10 m from MERRA-2 | Weaker easterly trade winds | MERRA-2 is a reanalysis, not a direct measurement |
| Precipitation | Satellite-estimated rainfall from GPM IMERG | More rain in northern Peru | Late Run is preliminary; Lima is normally very dry |
| Chlorophyll-a | Proxy for surface phytoplankton from MODIS-Aqua | Lower concentration under weaker upwelling | The series ends in November 2025 and contains cloud-related gaps |

These are hypotheses to inspect, not guaranteed outcomes.

## Technology and architecture

- Java 21 and Spring Boot 4
- Apache Commons Math and Commons CSV
- Plotly.js served locally by the application
- Maven for builds and JUnit for backend tests
- Playwright for browser-level tests
- Docker and a Render Blueprint for deployment

The CSV datasets are packaged in the executable JAR. Earthdata credentials are only needed to acquire or refresh source data; they are not required to run the deployed application.

## Data

| Signal | Product | Declared coverage |
| --- | --- | --- |
| Sea-surface temperature | MODIS-Aqua nighttime SST | July 2002 – August 2026 |
| Chlorophyll-a | MODIS-Aqua chlorophyll-a | July 2002 – November 2025 |
| Daily precipitation | GPM IMERG v07 Late Run | January 2002 – November 2026 |
| Validation precipitation | GPM IMERG v07 Final Run | January 2002 – September 2025 |
| Eastward wind | MERRA-2 10 m eastward wind | January 2002 – September 2026 |

Download metadata and geographic bounding boxes are documented in [docs/CSV_DATA.md](docs/CSV_DATA.md). Missing values remain missing and are never replaced with zero. Daily precipitation is aggregated to monthly totals and marked partial when a month is incomplete.

## Statistical methods

- **Monthly anomalies:** observation minus the climatological mean for the same calendar month.
- **Theil–Sen slope:** median of pairwise slopes, reported per decade.
- **Mann–Kendall test:** monotonic-trend test with tie and continuity corrections.
- **Spearman correlation:** rank association after monthly alignment, optionally shifted by 0–4 months.
- **Current-event comparison:** z-score, percentile, and empirical return period relative to the historical baseline.

Small controlled fixtures were checked independently with NumPy and SciPy. See [docs/validacion-estadisticos.md](docs/validacion-estadisticos.md) for the reproducible calculations.

## Run locally

### Requirements

- Java 21 or newer
- Maven 3.9+ or the included Maven wrapper
- Node.js only when running browser tests

Start the application from the repository root:

```bash
./mvnw spring-boot:run
```

If the wrapper does not have execute permission on a Unix-like system:

```bash
chmod +x mvnw
./mvnw spring-boot:run
```

Open <http://localhost:8080>.

## Run the tests

Backend and integration tests:

```bash
./mvnw test
```

Browser tests require the application to be running. Install Playwright once:

```bash
npm install
npx playwright install chromium
```

Then run:

```bash
BASE_URL=http://localhost:8080 npm run test:e2e
```

## API

`GET /api/variables` returns the catalog and valid variable identifiers.

| Route | Parameters | Result |
| --- | --- | --- |
| `/api/analisis` | `var`, `desde`, `hasta` | Combined series, anomalies, trend, event comparison, and lag relationships |
| `/api/serie` | `var`, optional `desde`, `hasta` | Filtered time series |
| `/api/tendencia` | `var`, optional `desde`, `hasta` | Theil–Sen change per decade, interval, and Mann–Kendall p-value |
| `/api/correlacion` | `a`, `b`, `rezago=0..4` | Spearman coefficient, approximate p-value, and valid-pair count |
| `/api/evento` | `var` | 2026 comparison against the 2002–2025 annual baseline |
| `/api/validacion-lluvia` | none | Aggregated Late Run versus monthly Final Run precipitation |
| `/api/analogias` | none | Temperature–chlorophyll associations for historical event windows |

Dates use ISO `YYYY-MM-DD` format. A lag compares `a(t)` with `b(t + lag)`. Missing and partial observations do not enter trends or correlation pairs.

## Docker

Build and run the same container used by Render:

```bash
docker build -t climate-detective .
docker run --rm -p 8080:10000 -e PORT=10000 climate-detective
```

Verify readiness at <http://localhost:8080/actuator/health>.

## Deploy to Render

The repository includes a [Render Blueprint](render.yaml) for a free Docker web service:

1. Push `Dockerfile`, `.dockerignore`, and `render.yaml` to the `main` branch.
2. Open `https://dashboard.render.com/blueprint/new?repo=https://github.com/dayDPG/inkatech26`.
3. Review the planned `detective-del-clima` web service and apply the Blueprint.
4. Wait for the deploy to become `Live` and verify `/actuator/health` and `/`.

Render injects `PORT`; Spring Boot reads it through `server.port=${PORT:8080}`. No application secret is required for the bundled datasets. Never commit `.env` or Earthdata credentials.

## Limitations

- Monthly climate observations are autocorrelated, so asymptotic p-values can be optimistic.
- Trends depend on the selected period and can be strongly influenced by major events; they are not estimates of background ocean warming.
- IMERG Late Run precipitation is preliminary and less accurate than Final Run.
- Chlorophyll-a has no 2026 observations, so it cannot establish a biological response for the current period.
- A significant correlation does not establish causality or predict impacts.
- The record contains few strong events, so event analogies are illustrative.
- The statistical checks validate software formulas and edge cases, not the physical accuracy of the satellite products.

## References

- [NASA Giovanni](https://giovanni.gsfc.nasa.gov/giovanni/) — source interface for the MODIS-Aqua, GPM IMERG, and MERRA-2 time series.
- [ENFEN technical reports](https://enfen.imarpe.gob.pe/informes-tecnicos/) — official Peruvian monitoring and guidance for El Niño.
- [NOAA Climate Prediction Center ENSO resources](https://www.cpc.ncep.noaa.gov/products/analysis_monitoring/enso_advisory/) — operational ENSO monitoring and Niño-region context.
- [KNMI Climate Explorer](https://climexp.knmi.nl/) — climate indices and independent exploratory datasets.
- Escudero, L., Xu, H., & Ledesma, J. (2025). [*Climatology of chlorophyll-a off the Peruvian coast using SeaWiFS (1997–2010)*](https://doi.org/10.54140/raop.v7i2.92).
- Echevin et al. (2017). [*Impacts of El Niño events on the Peruvian upwelling system productivity*](https://doi.org/10.1002/2016JC012439).
- [NumPy documentation](https://numpy.org/doc/) and [SciPy statistical functions](https://docs.scipy.org/doc/scipy/reference/stats.html) — independent references for controlled statistical fixtures.

## Responsible use of AI

The team used AI-assisted tools to support test development, interface refinement, and implementation review. Statistical formulas are covered by automated tests and independently reproducible fixtures; scientific interpretation remains subject to the limitations above.
