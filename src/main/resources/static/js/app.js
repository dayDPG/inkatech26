(() => {
    const GROUP_LABELS = {
        "sea-surface-temperature": "Temperatura del mar",
        "precipitation": "Precipitación",
        "eastward-wind": "Viento zonal",
        "chlorophyll-a": "Clorofila-a"
    };
    const GROUP_ORDER = Object.keys(GROUP_LABELS);
    const state = { variables: [], selected: null };
    const elements = {
        plotlyStatus: document.querySelector("#plotly-status"),
        form: document.querySelector("#series-form"),
        variable: document.querySelector("#variable-select"),
        region: document.querySelector("#region-select"),
        from: document.querySelector("#date-from"),
        to: document.querySelector("#date-to"),
        submit: document.querySelector("#series-submit"),
        message: document.querySelector("#series-message"),
        chart: document.querySelector("#series-chart"),
        cards: document.querySelector("#variable-cards"),
        relationForm: document.querySelector("#relation-form"),
        relationPair: document.querySelector("#relation-pair"),
        lag: document.querySelector("#lag-select"),
        relationSubmit: document.querySelector("#relation-submit"),
        dialog: document.querySelector("#variable-dialog"),
        dialogClose: document.querySelector("#dialog-close")
    };

    function setPlotlyStatus() {
        if (!elements.plotlyStatus) return;
        if (typeof window.Plotly === "undefined") {
            elements.plotlyStatus.textContent = "Motor gráfico no disponible";
            elements.plotlyStatus.dataset.state = "error";
            document.documentElement.dataset.plotly = "error";
            return;
        }
        elements.plotlyStatus.textContent = `Plotly ${window.Plotly.version} · listo`;
        elements.plotlyStatus.dataset.state = "ready";
        document.documentElement.dataset.plotly = "ready";
    }

    async function fetchJson(path) {
        const response = await fetch(path, { headers: { Accept: "application/json" } });
        if (response.ok) return response.json();
        let message = `La solicitud falló (${response.status})`;
        try {
            const body = await response.json();
            if (body.error) message = body.error;
        } catch (ignored) {
            // The HTTP status remains useful when the response has no JSON body.
        }
        throw new Error(message);
    }

    function setButtonState(button, nextState, label) {
        button.dataset.state = nextState;
        button.disabled = nextState === "loading";
        button.setAttribute("aria-disabled", String(button.disabled));
        button.querySelector(".action-button__label").textContent = label;
    }

    function variablesForGroup(group) {
        return state.variables.filter(variable => variable.grupo === group);
    }

    function populateVariableGroups() {
        elements.variable.replaceChildren();
        GROUP_ORDER.filter(group => variablesForGroup(group).length > 0).forEach(group => {
            elements.variable.add(new Option(GROUP_LABELS[group], group));
        });
        elements.variable.disabled = false;
        populateRegions();
    }

    function regionLabel(variable) {
        const resolution = variable.resolucion === "DIARIA" ? "diaria" : "mensual";
        const finalVersion = variable.id.endsWith("-final") ? " · final" : "";
        return `${variable.region.nombre} · ${resolution}${finalVersion}`;
    }

    function populateRegions() {
        const options = variablesForGroup(elements.variable.value);
        elements.region.replaceChildren();
        options.forEach(variable => elements.region.add(new Option(regionLabel(variable), variable.id)));
        elements.region.disabled = options.length === 0;
        selectConcreteVariable();
    }

    function selectConcreteVariable() {
        state.selected = state.variables.find(variable => variable.id === elements.region.value) ?? null;
        if (!state.selected) return;
        const { desde, hasta } = state.selected.periodo;
        elements.from.min = desde;
        elements.from.max = hasta;
        elements.to.min = desde;
        elements.to.max = hasta;
        elements.from.value = desde;
        elements.to.value = hasta;
        clearValidation();
    }

    function clearValidation() {
        [elements.variable, elements.region, elements.from, elements.to]
                .forEach(control => control.removeAttribute("aria-invalid"));
        elements.message.textContent = "";
        delete elements.message.dataset.state;
    }

    function validatePeriod() {
        clearValidation();
        if (!state.selected || !elements.from.value || !elements.to.value) {
            elements.message.textContent = "Completa la variable, la región y las dos fechas.";
            elements.message.dataset.state = "error";
            return false;
        }
        if (elements.from.value > elements.to.value) {
            elements.from.setAttribute("aria-invalid", "true");
            elements.to.setAttribute("aria-invalid", "true");
            elements.message.textContent = "El periodo es inválido: la fecha inicial debe ir antes de la final.";
            elements.message.dataset.state = "error";
            return false;
        }
        return true;
    }

    function queryForSelected() {
        return new URLSearchParams({
            var: state.selected.id,
            desde: elements.from.value,
            hasta: elements.to.value
        });
    }

    async function loadAnalysis() {
        if (!validatePeriod()) return;
        setButtonState(elements.submit, "loading", "Cargando datos");
        elements.message.textContent = "Consultando serie y tendencia…";
        try {
            const query = queryForSelected();
            const series = await fetchJson(`/api/serie?${query}`);
            await renderChart(series, null);
            try {
                const trend = await fetchJson(`/api/tendencia?${query}`);
                await renderChart(series, trend);
                renderTrend(trend);
            } catch (trendError) {
                renderTrendUnavailable(trendError.message);
            }
            elements.message.textContent = `${series.puntos.length.toLocaleString("es-PE")} observaciones cargadas.`;
            setButtonState(elements.submit, "success", "Análisis actualizado");
        } catch (error) {
            elements.message.textContent = error.message;
            elements.message.dataset.state = "error";
            setButtonState(elements.submit, "error", "Intentar de nuevo");
        }
    }

    function cssToken(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function decimalYear(date) {
        const current = new Date(`${date}T00:00:00Z`);
        const start = Date.UTC(current.getUTCFullYear(), 0, 1);
        const end = Date.UTC(current.getUTCFullYear() + 1, 0, 1);
        return current.getUTCFullYear() + (current.getTime() - start) / (end - start);
    }

    function trendTrace(points, trend) {
        if (!trend || points.length < 2) return null;
        const years = points.map(point => decimalYear(point.fecha));
        const centerYear = years.reduce((sum, year) => sum + year, 0) / years.length;
        const meanValue = points.reduce((sum, point) => sum + point.valor, 0) / points.length;
        const annualSlope = trend.pendientePorDecada / 10.0;
        const first = points[0];
        const last = points[points.length - 1];
        return {
            x: [first.fecha, last.fecha],
            y: [
                meanValue + annualSlope * (decimalYear(first.fecha) - centerYear),
                meanValue + annualSlope * (decimalYear(last.fecha) - centerYear)
            ],
            type: "scatter",
            mode: "lines",
            name: "Tendencia",
            line: { color: cssToken("--plot-warning"), width: 3, dash: "dash" },
            hovertemplate: "%{y:.3g}<extra>Tendencia</extra>"
        };
    }

    async function renderChart(series, trend) {
        if (typeof window.Plotly === "undefined") return;
        const points = series.puntos.filter(point => Number.isFinite(point.valor));
        if (points.length === 0) throw new Error("La serie no contiene valores válidos en ese periodo.");
        const traces = [{
            x: points.map(point => point.fecha),
            y: points.map(point => point.valor),
            type: "scatter",
            mode: "lines",
            name: "Serie observada",
            line: { color: cssToken("--plot-accent"), width: 2 },
            hovertemplate: `%{x|%Y-%m-%d}<br>%{y:.4g} ${series.unidad}<extra></extra>`
        }];
        const fitted = trendTrace(points, trend);
        if (fitted) traces.push(fitted);
        elements.chart.replaceChildren();
        await window.Plotly.newPlot(elements.chart, traces, {
            autosize: true,
            paper_bgcolor: cssToken("--plot-paper"),
            plot_bgcolor: cssToken("--plot-paper"),
            font: { family: cssToken("--font-body"), color: cssToken("--plot-ink") },
            margin: { l: 64, r: 24, t: 48, b: 56 },
            xaxis: { title: "Fecha", gridcolor: cssToken("--plot-rule"), zeroline: false },
            yaxis: { title: series.unidad, gridcolor: cssToken("--plot-rule"), zeroline: false },
            legend: { orientation: "h", y: 1.12, x: 0 }
        }, {
            responsive: true,
            displayModeBar: window.matchMedia("(min-width: 40rem)").matches,
            displaylogo: false,
            modeBarButtonsToRemove: ["lasso2d", "select2d"]
        });
    }

    function formatNumber(value, digits = 3) {
        return new Intl.NumberFormat("es-PE", { maximumSignificantDigits: digits }).format(value);
    }

    function renderTrend(trend) {
        const significant = trend.valorP <= 0.05;
        const unit = state.selected.unidad;
        document.querySelector("#trend-title").textContent = state.selected.nombre;
        document.querySelector("#trend-slope").textContent = `${formatNumber(trend.pendientePorDecada)} ${unit}/década`;
        document.querySelector("#trend-pvalue").textContent = formatNumber(trend.valorP, 4);
        document.querySelector("#trend-interval").textContent =
                `${formatNumber(trend.intervaloConfianzaInferior)} a ${formatNumber(trend.intervaloConfianzaSuperior)}`;
        const light = document.querySelector("#significance-light");
        light.dataset.state = significant ? "significant" : "not-significant";
        light.textContent = significant ? "Evidencia significativa" : "No significativa";
        const direction = trend.pendientePorDecada >= 0 ? "aumenta" : "disminuye";
        document.querySelector("#trend-explanation").textContent = significant
                ? `La serie ${direction} y la prueba Mann-Kendall detecta una tendencia significativa (p ≤ 0,05).`
                : `La pendiente estimada ${direction}, pero p > 0,05: no hay evidencia suficiente para afirmar una tendencia.`;
    }

    function renderTrendUnavailable(message) {
        document.querySelector("#trend-title").textContent = "Tendencia no disponible";
        document.querySelector("#trend-slope").textContent = "—";
        document.querySelector("#trend-pvalue").textContent = "—";
        document.querySelector("#trend-interval").textContent = "—";
        const light = document.querySelector("#significance-light");
        light.dataset.state = "not-significant";
        light.textContent = "Sin resultado";
        document.querySelector("#trend-explanation").textContent = message;
    }

    function createVariableCard(group) {
        const variables = variablesForGroup(group);
        const representative = variables[0];
        const article = document.createElement("article");
        article.className = "variable-card";
        const content = document.createElement("div");
        const title = document.createElement("h3");
        title.textContent = GROUP_LABELS[group];
        const definition = document.createElement("p");
        definition.textContent = representative.definicion;
        const meta = document.createElement("span");
        meta.className = "variable-card__meta";
        meta.textContent = `${representative.unidad} · ${variables.length} ${variables.length === 1 ? "serie" : "series"}`;
        const help = document.createElement("button");
        help.type = "button";
        help.className = "variable-card__help";
        help.textContent = "?";
        help.setAttribute("aria-label", `Abrir ficha de ${GROUP_LABELS[group]}`);
        help.addEventListener("click", () => openVariableDialog(group));
        content.append(title, definition, meta);
        article.append(content, help);
        return article;
    }

    function renderVariableCards() {
        elements.cards.replaceChildren(...GROUP_ORDER
                .filter(group => variablesForGroup(group).length > 0)
                .map(createVariableCard));
    }

    function appendDetail(list, term, description) {
        const dt = document.createElement("dt");
        dt.textContent = term;
        const dd = document.createElement("dd");
        dd.textContent = description;
        list.append(dt, dd);
    }

    function openVariableDialog(group) {
        const variables = variablesForGroup(group);
        const representative = variables[0];
        document.querySelector("#dialog-title").textContent = GROUP_LABELS[group];
        document.querySelector("#dialog-definition").textContent = representative.definicion;
        const details = document.querySelector("#dialog-details");
        details.replaceChildren();
        appendDetail(details, "Unidad", representative.unidad);
        appendDetail(details, "Dataset", [...new Set(variables.map(variable => variable.dataset))].join(" · "));
        appendDetail(details, "Regiones", variables.map(variable => variable.region.nombre).join(" · "));
        appendDetail(details, "Cobertura", `${representative.periodo.desde} a ${representative.periodo.hasta}`);
        document.querySelector("#dialog-expectation").textContent = representative.expectativaElNino;
        [document.querySelector(".site-header"), document.querySelector("main"), document.querySelector(".site-footer")]
                .forEach(element => { element.inert = true; });
        elements.dialog.showModal();
        elements.dialogClose.focus();
    }

    function closeVariableDialog() {
        elements.dialog.close();
        [document.querySelector(".site-header"), document.querySelector("main"), document.querySelector(".site-footer")]
                .forEach(element => { element.inert = false; });
    }

    function correlationDescription(coefficient) {
        const magnitude = Math.abs(coefficient);
        const strength = magnitude < 0.2 ? "muy débil"
                : magnitude < 0.4 ? "débil"
                : magnitude < 0.6 ? "moderada"
                : magnitude < 0.8 ? "fuerte" : "muy fuerte";
        const direction = coefficient >= 0 ? "positiva" : "negativa";
        return `${strength} y ${direction}`;
    }

    async function loadCorrelation() {
        const [a, b] = elements.relationPair.value.split("|");
        const lag = elements.lag.value;
        setButtonState(elements.relationSubmit, "loading", "Calculando");
        try {
            const query = new URLSearchParams({ a, b, rezago: lag });
            const result = await fetchJson(`/api/correlacion?${query}`);
            document.querySelector("#correlation-value").textContent = `ρ = ${formatNumber(result.coeficiente, 3)}`;
            const significance = result.valorP <= 0.05
                    ? "La asociación es estadísticamente significativa."
                    : "La asociación no es estadísticamente significativa.";
            document.querySelector("#correlation-reading").textContent =
                    `Relación ${correlationDescription(result.coeficiente)}. ${significance}`;
            document.querySelector("#correlation-meta").textContent =
                    `Spearman · p = ${formatNumber(result.valorP, 4)} · ${result.numeroPares} pares · rezago ${result.rezagoMeses} meses`;
            setButtonState(elements.relationSubmit, "success", "Relación calculada");
        } catch (error) {
            document.querySelector("#correlation-value").textContent = "ρ = —";
            document.querySelector("#correlation-reading").textContent = error.message;
            document.querySelector("#correlation-meta").textContent = "No se pudo completar el cálculo.";
            setButtonState(elements.relationSubmit, "error", "Intentar de nuevo");
        }
    }

    async function initialize() {
        setPlotlyStatus();
        try {
            state.variables = await fetchJson("/api/variables");
            populateVariableGroups();
            renderVariableCards();
            await loadAnalysis();
        } catch (error) {
            elements.message.textContent = error.message;
            elements.message.dataset.state = "error";
            setButtonState(elements.submit, "error", "Reintentar carga");
        }
    }

    elements.variable.addEventListener("change", populateRegions);
    elements.region.addEventListener("change", selectConcreteVariable);
    elements.form.addEventListener("submit", event => {
        event.preventDefault();
        loadAnalysis();
    });
    elements.relationForm.addEventListener("submit", event => {
        event.preventDefault();
        loadCorrelation();
    });
    elements.dialogClose.addEventListener("click", closeVariableDialog);
    elements.dialog.addEventListener("click", event => {
        if (event.target === elements.dialog) closeVariableDialog();
    });
    elements.dialog.addEventListener("close", () => {
        [document.querySelector(".site-header"), document.querySelector("main"), document.querySelector(".site-footer")]
                .forEach(element => { element.inert = false; });
    });

    initialize();
})();
