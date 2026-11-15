(() => {
    const GROUP_LABELS = {
        "sea-surface-temperature": "Temperatura del mar",
        "precipitation": "Precipitación",
        "eastward-wind": "Viento zonal",
        "chlorophyll-a": "Clorofila-a"
    };
    const DEMOS = {
        "elnino-2017": {
            variable: "sea-surface-temperature",
            region: "sea-surface-temperature-total",
            desde: "2017-01-01",
            hasta: "2017-12-31"
        },
        "elnino-2023-24": {
            variable: "sea-surface-temperature",
            region: "sea-surface-temperature-total",
            desde: "2023-01-01",
            hasta: "2024-11-30"
        }
    };
    const GROUP_ORDER = Object.keys(GROUP_LABELS);
    const estado = { variable: "", region: "", desde: "", hasta: "" };
    const catalogo = [];
    const elements = {
        plotlyStatus: document.querySelector("#plotly-status"),
        form: document.querySelector("#series-form"),
        variable: document.querySelector("#variable-select"),
        region: document.querySelector("#region-select"),
        from: document.querySelector("#date-from"),
        to: document.querySelector("#date-to"),
        submit: document.querySelector("#series-submit"),
        message: document.querySelector("#series-message"),
        selectedTitle: document.querySelector("#selected-variable-title"),
        selectedExplanation: document.querySelector("#selected-variable-explanation"),
        selectedReading: document.querySelector("#selected-variable-reading"),
        seriesChart: document.querySelector("#series-chart"),
        anomalyChart: document.querySelector("#anomaly-chart"),
        relations: document.querySelector("#relations-list"),
        presets: document.querySelectorAll("[data-preset]")
    };

    function cssToken(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    async function fetchJson(path) {
        const response = await fetch(path, { headers: { Accept: "application/json" } });
        if (response.ok) return response.json();
        let message = `La solicitud falló (${response.status})`;
        try {
            const body = await response.json();
            message = body.mensaje || body.message || body.error || message;
        } catch (ignored) {
            // Use the HTTP status when the server does not return JSON.
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
        return catalogo.filter(variable => variable.grupo === group);
    }

    function regionLabel(variable) {
        const resolution = variable.resolucion === "DIARIA" ? "diaria · mensual" : "mensual";
        const finalVersion = variable.id.endsWith("-final") ? " · final" : "";
        return `${variable.region.nombre} · ${resolution}${finalVersion}`;
    }

    function selectedVariable() {
        return catalogo.find(variable => variable.id === elements.region.value) ?? null;
    }

    function showVariableExplanation(variable) {
        if (!variable) return;
        elements.selectedTitle.textContent = variable.nombre;
        elements.selectedExplanation.textContent = variable.definicion;
        elements.selectedReading.textContent = variable.expectativaElNino;
    }

    function setPeriodFromVariable(variable) {
        const { desde, hasta } = variable.periodo;
        elements.from.min = desde;
        elements.from.max = hasta;
        elements.to.min = desde;
        elements.to.max = hasta;
        elements.from.value = desde;
        elements.to.value = hasta;
        estado.desde = desde;
        estado.hasta = hasta;
    }

    function populateRegions(preferredId = null) {
        const options = variablesForGroup(elements.variable.value);
        const currentRegion = preferredId || elements.region.value;
        elements.region.replaceChildren();
        options.forEach(variable => elements.region.add(new Option(regionLabel(variable), variable.id)));
        elements.region.disabled = options.length === 0;
        const selected = options.find(variable => variable.id === currentRegion) || options[0];
        if (!selected) return;
        elements.region.value = selected.id;
        estado.variable = selected.grupo;
        estado.region = selected.id;
        setPeriodFromVariable(selected);
        showVariableExplanation(selected);
    }

    function populateVariables() {
        elements.variable.replaceChildren();
        GROUP_ORDER.filter(group => variablesForGroup(group).length > 0).forEach(group => {
            elements.variable.add(new Option(GROUP_LABELS[group], group));
        });
        elements.variable.disabled = false;
        populateRegions();
    }

    function captureSelection() {
        estado.variable = elements.variable.value;
        estado.region = elements.region.value;
        estado.desde = elements.from.value;
        estado.hasta = elements.to.value;
    }

    function validateSelection() {
        captureSelection();
        [elements.variable, elements.region, elements.from, elements.to]
            .forEach(control => control.removeAttribute("aria-invalid"));
        elements.message.textContent = "";
        delete elements.message.dataset.state;

        if (!estado.variable || !estado.region || !estado.desde || !estado.hasta) {
            elements.message.textContent = "Completa variable, región y fechas antes de analizar.";
            elements.message.dataset.state = "error";
            return false;
        }
        if (estado.desde > estado.hasta) {
            elements.from.setAttribute("aria-invalid", "true");
            elements.to.setAttribute("aria-invalid", "true");
            elements.message.textContent = "La fecha inicial debe ser anterior a la fecha final.";
            elements.message.dataset.state = "error";
            return false;
        }
        return true;
    }

    function formatNumber(value, digits = 3) {
        return new Intl.NumberFormat("es-PE", { maximumSignificantDigits: digits }).format(value);
    }

    async function drawChart(target, serie, title, zeroLine = false) {
        if (typeof window.Plotly === "undefined") return;
        const points = serie.puntos.filter(point => Number.isFinite(point.valor));
        if (points.length === 0) {
            target.replaceChildren();
            const empty = document.createElement("p");
            empty.className = "chart-empty";
            empty.textContent = "No hay observaciones válidas en este periodo.";
            target.append(empty);
            return;
        }

        target.replaceChildren();
        await window.Plotly.newPlot(target, [{
            x: points.map(point => point.fecha),
            y: points.map(point => point.valor),
            type: "scatter",
            mode: "lines",
            name: title,
            line: { color: cssToken("--plot-accent"), width: 2 },
            hovertemplate: `%{x|%Y-%m-%d}<br>%{y:.4g} ${serie.unidad}<extra></extra>`
        }], {
            autosize: true,
            paper_bgcolor: cssToken("--plot-paper"),
            plot_bgcolor: cssToken("--plot-paper"),
            font: { family: cssToken("--font-body"), color: cssToken("--plot-ink") },
            margin: { l: 64, r: 24, t: 32, b: 56 },
            xaxis: { title: "Fecha", gridcolor: cssToken("--plot-rule"), zeroline: false },
            yaxis: {
                title: serie.unidad,
                gridcolor: cssToken("--plot-rule"),
                zeroline: zeroLine,
                zerolinecolor: cssToken("--plot-warning")
            },
            showlegend: false
        }, {
            responsive: true,
            displayModeBar: window.matchMedia("(min-width: 40rem)").matches,
            displaylogo: false,
            modeBarButtonsToRemove: ["lasso2d", "select2d"]
        });
    }

    function renderTrend(analisis) {
        const trend = analisis.tendencia;
        if (!trend) {
            document.querySelector("#trend-title").textContent = "Tendencia no disponible";
            document.querySelector("#trend-slope").textContent = "—";
            document.querySelector("#trend-pvalue").textContent = "—";
            document.querySelector("#trend-interval").textContent = "—";
            document.querySelector("#significance-light").textContent = "Sin resultado";
            document.querySelector("#significance-light").dataset.state = "unavailable";
            document.querySelector("#trend-explanation").textContent = analisis.mensajeTendencia;
            return;
        }

        const significant = trend.valorP < 0.05;
        document.querySelector("#trend-title").textContent = selectedVariable().nombre;
        document.querySelector("#trend-slope").textContent =
            `${formatNumber(trend.pendientePorDecada)} ${analisis.anomalias.unidad}/década`;
        document.querySelector("#trend-pvalue").textContent = formatNumber(trend.valorP, 4);
        document.querySelector("#trend-interval").textContent =
            `${formatNumber(trend.intervaloConfianzaInferior)} a ${formatNumber(trend.intervaloConfianzaSuperior)}`;
        const light = document.querySelector("#significance-light");
        light.dataset.state = significant ? "significant" : "not-significant";
        light.textContent = significant ? "Evidencia de tendencia" : "Sin evidencia suficiente";
        const direction = trend.pendientePorDecada >= 0 ? "aumenta" : "disminuye";
        document.querySelector("#trend-explanation").textContent = significant
            ? `La serie de anomalías ${direction}; Mann-Kendall encuentra evidencia de tendencia (p < 0,05). No es un pronóstico.`
            : `${analisis.mensajeTendencia || `La pendiente sugiere que las anomalías ${direction}, pero p ≥ 0,05: no hay evidencia suficiente para afirmar una tendencia.`} Esto no demuestra que no exista cambio.`;
    }

    function renderEvent(analisis) {
        const event = analisis.evento;
        if (!event) {
            document.querySelector("#event-value").textContent = "—";
            document.querySelector("#event-zscore").textContent = "—";
            document.querySelector("#event-percentile").textContent = "—";
            document.querySelector("#event-message").textContent = "No hay un dato completo para comparar.";
            return;
        }
        document.querySelector("#event-title").textContent = `Comparación para ${selectedVariable().nombre}`;
        document.querySelector("#event-value").textContent =
            `${event.fecha} · ${formatNumber(event.valor)} ${analisis.serie.unidad}`;
        document.querySelector("#event-zscore").textContent =
            event.zScore === null ? "—" : formatNumber(event.zScore);
        document.querySelector("#event-percentile").textContent =
            event.percentil === null ? "—" : `${formatNumber(event.percentil, 4)} %`;
        document.querySelector("#event-message").textContent =
            event.mensaje || `Comparado con ${event.numeroDeMesesHistoricos} observaciones del mismo mes. Describe el registro histórico, no una probabilidad futura.`;
    }

    function renderRelations(analisis) {
        const container = elements.relations;
        container.replaceChildren();
        if (analisis.relaciones.length === 0) {
            container.textContent = "No hay relaciones disponibles para esta variable.";
            return;
        }

        analisis.relaciones.forEach(relacion => {
            const article = document.createElement("article");
            article.className = "relation-card";
            const title = document.createElement("h3");
            title.textContent = relacion.nombre;
            article.append(title);
            const best = document.createElement("p");
            best.className = "relation-card__best";
            best.textContent = relacion.mejorRezago === null
                ? "No se pudo identificar un rezago con datos suficientes."
                : `Mayor asociación observada: ${relacion.mejorRezago === 0 ? "mismo mes" : `${relacion.mejorRezago} ${relacion.mejorRezago === 1 ? "mes" : "meses"} después`}.`;
            article.append(best);

            const rows = document.createElement("div");
            rows.className = "lag-list";
            relacion.rezagos.forEach(rezago => rows.append(renderLag(rezago)));
            article.append(rows);
            container.append(article);
        });
    }

    function renderLag(rezago) {
        const row = document.createElement("div");
        row.className = "lag-row";
        const label = document.createElement("span");
        label.className = "lag-row__label";
        label.textContent = rezago.meses === 0 ? "Mismo mes" : `+${rezago.meses} ${rezago.meses === 1 ? "mes" : "meses"}`;
        const track = document.createElement("span");
        track.className = "lag-bar";
        if (rezago.rho !== null) {
            const bar = document.createElement("span");
            bar.className = `lag-bar__value ${rezago.rho < 0 ? "lag-bar__value--negative" : ""}`;
            bar.style.left = rezago.rho < 0 ? `${50 - Math.abs(rezago.rho) * 50}%` : "50%";
            bar.style.width = `${Math.abs(rezago.rho) * 50}%`;
            track.append(bar);
        }
        const value = document.createElement("span");
        value.className = "lag-row__rho";
        value.textContent = rezago.rho === null ? "ρ = —" : `ρ = ${formatNumber(rezago.rho, 3)}`;
        const count = document.createElement("small");
        count.className = "lag-row__count";
        count.textContent = `n = ${rezago.numeroDePares}`;
        if (rezago.mensaje) row.title = rezago.mensaje;
        row.append(label, track, value, count);
        return row;
    }

    async function renderAnalysis(analisis) {
        await Promise.all([
            drawChart(elements.seriesChart, analisis.serie, "Serie observada"),
            drawChart(elements.anomalyChart, analisis.anomalias, "Anomalía mensual", true)
        ]);
        renderTrend(analisis);
        renderEvent(analisis);
        renderRelations(analisis);
        elements.message.textContent =
            `${analisis.serie.puntos.length.toLocaleString("es-PE")} observaciones en el periodo; resultados calculados con la misma selección.`;
        setButtonState(elements.submit, "success", "Análisis actualizado");
    }

    async function analizar() {
        if (!validateSelection()) return false;
        setButtonState(elements.submit, "loading", "Analizando");
        elements.message.textContent = "Calculando serie, tendencia, evento y relaciones…";
        const query = new URLSearchParams({
            var: estado.region,
            desde: estado.desde,
            hasta: estado.hasta
        });
        try {
            const analisis = await fetchJson(`/api/analisis?${query}`);
            await renderAnalysis(analisis);
            return true;
        } catch (error) {
            elements.message.textContent = error.message;
            elements.message.dataset.state = "error";
            setButtonState(elements.submit, "error", "Intentar de nuevo");
            return false;
        }
    }

    function aplicarPreset(id) {
        const preset = DEMOS[id];
        if (!preset) return;
        elements.variable.value = preset.variable;
        populateRegions(preset.region);
        elements.from.value = preset.desde;
        elements.to.value = preset.hasta;
        estado.variable = preset.variable;
        estado.region = preset.region;
        estado.desde = preset.desde;
        estado.hasta = preset.hasta;
        const menu = document.querySelector(".project-menu");
        menu.open = false;
        analizar();
    }

    function setPlotlyStatus() {
        if (typeof window.Plotly === "undefined") {
            elements.plotlyStatus.textContent = "Motor gráfico no disponible";
            elements.plotlyStatus.dataset.state = "error";
            return;
        }
        elements.plotlyStatus.textContent = `Plotly ${window.Plotly.version} · listo`;
        elements.plotlyStatus.dataset.state = "ready";
    }

    elements.variable.addEventListener("change", () => populateRegions());
    elements.region.addEventListener("change", () => {
        const variable = selectedVariable();
        if (!variable) return;
        estado.variable = variable.grupo;
        estado.region = variable.id;
        setPeriodFromVariable(variable);
        showVariableExplanation(variable);
    });
    elements.from.addEventListener("change", captureSelection);
    elements.to.addEventListener("change", captureSelection);
    elements.form.addEventListener("submit", event => {
        event.preventDefault();
        analizar();
    });
    elements.presets.forEach(button => {
        button.addEventListener("click", () => aplicarPreset(button.dataset.preset));
    });

    async function initialize() {
        setPlotlyStatus();
        try {
            catalogo.push(...await fetchJson("/api/variables"));
            populateVariables();
            await analizar();
        } catch (error) {
            elements.message.textContent = error.message;
            elements.message.dataset.state = "error";
            setButtonState(elements.submit, "error", "Reintentar carga");
        }
    }

    initialize();
})();
