const { expect, test } = require("@playwright/test");

const analysisCalls = new WeakMap();

test.beforeEach(async ({ page }) => {
    analysisCalls.set(page, 0);
    await page.route("**/api/analisis?*", route => {
        analysisCalls.set(page, analysisCalls.get(page) + 1);
        return route.continue();
    });
    await page.goto("/");
    await expect(page.locator("#plotly-status")).toContainText("listo", { timeout: 30000 });
    await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
});

test("una sola solicitud muestra serie, tendencia, evento y relaciones", async ({ page }) => {
    await expect(page.getByRole("heading", { name: "Analizador del fenómeno El Niño" })).toBeVisible();
    await expect(page.locator("#series-chart.js-plotly-plot")).toBeVisible();
    await expect(page.locator("#anomaly-chart.js-plotly-plot")).toBeVisible();
    await expect(page.locator("#variable-select")).toHaveValue("sea-surface-temperature");
    await expect(page.locator("#trend-title")).toHaveText("Temperatura superficial del mar");
    await expect(page.locator("#significance-light")).not.toHaveText("Sin resultado");
    await expect(page.locator("#event-zscore")).not.toHaveText("—");
    await expect(page.locator(".relation-card")).toHaveCount(3);
    await expect(page.locator(".lag-row")).toHaveCount(15);
    expect(analysisCalls.get(page)).toBe(1);
});

test("actualizar variable y fechas hace una solicitud conjunta", async ({ page }) => {
    await page.locator("#variable-select").selectOption("chlorophyll-a");
    await expect(page.locator("#region-select")).toHaveValue("chlorophyll-a-total");
    await expect(page.locator("#selected-variable-title")).toHaveText("Clorofila-a");
    await page.locator("#date-from").fill("2022-01-01");
    await page.locator("#date-to").fill("2024-11-30");
    await page.locator("#series-submit").click();

    await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
    await expect(page.locator(".relation-card")).toHaveCount(1);
    await expect(page.locator(".lag-row")).toHaveCount(5);
    await expect(page.locator("#trend-title")).toHaveText("Clorofila-a");
    expect(analysisCalls.get(page)).toBe(2);
});

test("cada demo actualiza el periodo, muestra la guía y sombrea el evento", async ({ page }) => {
    const presets = [
        {
            titulo: "El Niño costero de 2017",
            variable: "precipitation",
            region: "precipitation-norte",
            desde: "2015-01-01",
            hasta: "2018-12-31",
            inicio: "2017-01-01",
            fin: "2017-07-01"
        },
        {
            titulo: "El Niño 2023-24",
            variable: "sea-surface-temperature",
            region: "sea-surface-temperature-total",
            desde: "2021-01-01",
            hasta: "2024-11-30",
            inicio: "2023-03-01",
            fin: "2024-07-01"
        },
        {
            titulo: "El evento actual (2026)",
            variable: "sea-surface-temperature",
            region: "sea-surface-temperature-total",
            desde: "2024-01-01",
            hasta: "2026-08-31",
            inicio: "2026-02-01",
            fin: "2026-09-01"
        }
    ];

    for (const preset of presets) {
        await page.locator(".project-menu summary").click();
        await page.getByRole("button", { name: preset.titulo }).click();
        await expect(page.locator("#variable-select")).toHaveValue(preset.variable);
        await expect(page.locator("#region-select")).toHaveValue(preset.region);
        await expect(page.locator("#date-from")).toHaveValue(preset.desde);
        await expect(page.locator("#date-to")).toHaveValue(preset.hasta);
        await expect(page.locator("#demo-guide")).toBeVisible();
        await expect(page.locator("#demo-guide-title")).toHaveText(preset.titulo);
        await expect(page.locator("#demo-guide-description")).not.toBeEmpty();
        await expect(page.locator("#demo-guide-questions li")).toHaveCount(3);
        await expect(page.locator("#series-chart.js-plotly-plot")).toBeVisible();
        await expect(page.locator("#anomaly-chart.js-plotly-plot")).toBeVisible();
        await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
        await expect(page.locator(".project-menu")).not.toHaveAttribute("open", "");

        const chartShapes = await page.locator("#series-chart").evaluate(chart => ({
            shapes: chart._fullLayout.shapes,
            annotations: chart._fullLayout.annotations
        }));
        const anomalyShapes = await page.locator("#anomaly-chart").evaluate(chart => chart._fullLayout.shapes);
        expect(chartShapes.shapes).toHaveLength(1);
        expect(chartShapes.shapes[0]).toMatchObject({
            type: "rect",
            xref: "x",
            yref: "paper",
            x0: preset.inicio,
            x1: preset.fin,
            y0: 0,
            y1: 1,
            opacity: 0.15
        });
        expect(chartShapes.annotations[0].text).toBe("Evento (aprox.)");
        expect(anomalyShapes).toHaveLength(1);
    }

    expect(analysisCalls.get(page)).toBe(4);
});

test("al cargar una demo se limpian los resultados anteriores mientras se calcula", async ({ page }) => {
    await page.route(url => url.pathname === "/api/analisis"
        && url.searchParams.get("var") === "precipitation-norte", async route => {
        await new Promise(resolve => setTimeout(resolve, 1000));
        await route.continue();
    });

    await page.locator(".project-menu summary").click();
    await page.getByRole("button", { name: "El Niño costero de 2017" }).click();

    await expect(page.locator("#series-message"))
        .toHaveText("Calculando serie, tendencia, evento y relaciones…");
    await expect(page.locator("#series-chart")).toHaveText("Cargando serie…");
    await expect(page.locator("#anomaly-chart")).toHaveText("Cargando anomalías…");
    await expect(page.locator("#trend-title")).toHaveText("Calculando tendencia…");
    await expect(page.locator("#event-zscore")).toHaveText("—");
    await expect(page.locator("#relations-list")).toHaveText("Calculando relaciones…");

    await expect(page.locator("#series-message"))
        .toContainText("observaciones en el periodo", { timeout: 30000 });
    await expect(page.locator("#series-chart.js-plotly-plot")).toBeVisible();
});

test("cambiar manualmente las fechas quita la guía y el sombreado", async ({ page }) => {
    await page.locator(".project-menu summary").click();
    await page.getByRole("button", { name: "El Niño costero de 2017" }).click();
    await expect(page.locator("#demo-guide")).toBeVisible();
    await page.locator("#date-from").fill("2016-01-01");

    await expect(page.locator("#demo-guide")).toBeHidden();
    await expect.poll(() => page.locator("#series-chart").evaluate(chart => chart._fullLayout.shapes))
        .toEqual([]);
    await expect.poll(() => page.locator("#anomaly-chart").evaluate(chart => chart._fullLayout.shapes))
        .toEqual([]);
});

test("muestra un mensaje claro si una demo no tiene datos suficientes", async ({ page }) => {
    await page.route(url => url.pathname === "/api/analisis"
        && url.searchParams.get("var") === "precipitation-norte", route => route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({ mensaje: "No hay datos de la variable en el periodo seleccionado." })
    }));

    await page.locator(".project-menu summary").click();
    await page.getByRole("button", { name: "El Niño costero de 2017" }).click();
    await expect(page.locator("#series-message"))
        .toContainText("Datos insuficientes para completar el análisis.");
    await expect(page.locator("#series-chart"))
        .toContainText("No hay observaciones suficientes para mostrar este gráfico.");
    await expect(page.locator("#demo-guide")).toBeVisible();
});

test("la tendencia de lluvia usa la unidad mensual y las relaciones comparten el rango", async ({ page }) => {
    await page.locator("#variable-select").selectOption("precipitation");
    await expect(page.locator("#region-select")).toHaveValue("precipitation-norte");
    await page.locator("#date-from").fill("2017-01-01");
    await page.locator("#date-to").fill("2024-11-30");
    await page.locator("#series-submit").click();

    await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
    await expect(page.locator("#trend-slope")).toContainText("mm/mes/década");
    await expect(page.locator(".relation-card")).toHaveCount(1);
    await expect(page.locator(".lag-row")).toHaveCount(5);
});

test("muestra la lectura estadística junto al gráfico en escritorio y apila en móvil", async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    const chart = await page.locator("#series-chart").boundingBox();
    const trend = await page.locator(".trend-panel").boundingBox();
    expect(chart.x + chart.width).toBeLessThan(trend.x);

    const variable = await page.locator("#variable-select").boundingBox();
    const region = await page.locator("#region-select").boundingBox();
    const from = await page.locator("#date-from").boundingBox();
    const to = await page.locator("#date-to").boundingBox();
    const signalNote = await page.locator(".selection-note").boundingBox();
    const signalGuide = await page.locator(".signal-guide").boundingBox();
    expect(Math.abs(variable.y - region.y)).toBeLessThanOrEqual(1);
    expect(Math.abs(variable.y - from.y)).toBeLessThanOrEqual(1);
    expect(Math.abs(from.y - to.y)).toBeLessThanOrEqual(1);
    expect(Math.abs(variable.height - from.height)).toBeLessThanOrEqual(1);
    expect(signalNote.height).toBeLessThan(160);
    expect(signalGuide.height).toBeGreaterThan(100);

    await page.setViewportSize({ width: 390, height: 844 });
    const pageWidth = await page.locator("html").evaluate(element => element.scrollWidth);
    expect(pageWidth).toBeLessThanOrEqual(390);
    await expect(page.locator("#series-chart")).toBeVisible();
    await expect(page.locator("#anomaly-chart")).toBeVisible();
});

test("muestra un error de periodo inválido sin pedir el análisis", async ({ page }) => {
    const initialCalls = analysisCalls.get(page);
    await page.locator("#date-from").fill("2024-12-31");
    await page.locator("#date-to").fill("2024-01-01");
    await page.locator("#series-submit").click();

    await expect(page.locator("#series-message")).toContainText("debe ser anterior");
    expect(analysisCalls.get(page)).toBe(initialCalls);
});
