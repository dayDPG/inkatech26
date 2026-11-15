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

test("las demos son presets que actualizan la selección y analizan una vez", async ({ page }) => {
    await page.locator(".project-menu summary").click();
    await page.getByRole("button", { name: "El Niño costero 2017" }).click();
    await expect(page.locator("#date-from")).toHaveValue("2017-01-01");
    await expect(page.locator("#date-to")).toHaveValue("2017-12-31");
    await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
    await expect(page.locator("#event-value")).toContainText("2017");
    await expect(page.locator("#event-zscore")).not.toHaveText("—");
    await expect(page.locator(".project-menu")).not.toHaveAttribute("open", "");
    expect(analysisCalls.get(page)).toBe(2);

    await page.locator(".project-menu summary").click();
    await page.getByRole("button", { name: "El Niño 2023–24" }).click();
    await expect(page.locator("#date-from")).toHaveValue("2023-01-01");
    await expect(page.locator("#date-to")).toHaveValue("2024-11-30");
    await expect(page.locator("#series-message")).toContainText("observaciones en el periodo", { timeout: 30000 });
    await expect(page.locator("#event-value")).toContainText("2024");
    expect(analysisCalls.get(page)).toBe(3);
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
