(() => {
    const DEMOS = [
        {
            id: "elnino-2017",
            titulo: "El Niño costero de 2017",
            descripcion: "Explora cómo cambiaron las lluvias del norte antes, durante y después del evento costero de 2017.",
            variableId: "precipitation-norte",
            desde: "2015-01-01",
            hasta: "2018-12-31",
            evento: { inicio: "2017-01-01", fin: "2017-06-30" },
            preguntas: [
                "¿La anomalía se separa de cero durante el evento?",
                "¿Qué tan extremo es el mes más lluvioso?",
                "¿El viento y el mar se mueven juntos?"
            ]
        },
        {
            id: "elnino-2023-24",
            titulo: "El Niño 2023-24",
            descripcion: "Compara la temperatura superficial del mar antes y durante el episodio de 2023-24.",
            variableId: "sea-surface-temperature-total",
            desde: "2021-01-01",
            hasta: "2024-11-30",
            evento: { inicio: "2023-03-01", fin: "2024-06-30" },
            preguntas: [
                "¿La anomalía se mantiene por encima de cero durante el evento?",
                "¿En qué mes alcanza su valor más alto?",
                "¿Qué rezago muestra la mayor asociación con el viento?"
            ]
        },
        {
            id: "elnino-2026",
            titulo: "El evento actual (2026)",
            descripcion: "Observa la evolución reciente de la temperatura del mar y el tramo señalado desde febrero de 2026.",
            variableId: "sea-surface-temperature-total",
            desde: "2024-01-01",
            hasta: "2026-08-31",
            evento: { inicio: "2026-02-01", fin: null },
            preguntas: [
                "¿La anomalía aumenta desde febrero de 2026?",
                "¿Cómo se compara el último mes con su historial?",
                "¿Qué asociaciones aparecen en el periodo elegido?"
            ]
        }
    ];
    const GROUP_LABELS = {
        "sea-surface-temperature": "Temperatura del mar",
        "precipitation": "Precipitación",
        "eastward-wind": "Viento zonal",
        "chlorophyll-a": "Clorofila-a"
    };
    const GROUP_ORDER = Object.keys(GROUP_LABELS);
    const estado = { variable: "", region: "", desde: "", hasta: "" };
    const catalogo = [];
    let demoActiva = null;
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
        demoOptions: document.querySelector("#demo-options"),
        demoGuide: document.querySelector("#demo-guide"),
        demoTitle: document.querySelector("#demo-guide-title"),
        demoDescription: document.querySelector("#demo-guide-description"),
        demoQuestions: document.querySelector("#demo-guide-questions"),
        exitDemo: document.querySelector("#exit-demo")
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

    function renderDemoOptions() {
        elements.demoOptions.replaceChildren(...DEMOS.map(demo => {
            const button = document.createElement("button");
            button.type = "button";
            button.dataset.preset = demo.id;
            button.textContent = demo.titulo;
            return button;
        }));
    }

    function renderDemoGuide(demo) {
        elements.demoTitle.textContent = demo.titulo;
        elements.demoDescription.textContent = demo.descripcion;
        elements.demoQuestions.replaceChildren(...demo.preguntas.map(pregunta => {
            const item = document.createElement("li");
            item.textContent = pregunta;
            return item;
        }));
        elements.demoGuide.hidden = false;
    }

    function quitarDemo() {
        demoActiva = null;
        elements.demoGuide.hidden = true;
        if (typeof window.Plotly === "undefined") return;
        [elements.seriesChart, elements.anomalyChart]
            .filter(chart => chart.classList.contains("js-plotly-plot"))
            .forEach(chart => window.Plotly.relayout(chart, { shapes: [], annotations: [] }));
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

    function inicioMesSiguiente(fecha) {
        const [year, month] = fecha.split("-").map(Number);
        return new Date(Date.UTC(year, month, 1)).toISOString().slice(0, 10);
    }

    function rangoEvento(demo, serie) {
        if (!demo) return null;
        const ultimaFecha = serie.puntos
            .filter(point => Number.isFinite(point.valor))
            .reduce((latest, point) => point.fecha > latest ? point.fecha : latest, "");
        const fechaFin = demo.evento.fin || ultimaFecha;
        if (!fechaFin) return null;
        return {
            inicio: demo.evento.inicio,
            finExclusivo: inicioMesSiguiente(fechaFin)
        };
    }

    function decoracionEvento(evento) {
        if (!evento) return { shapes: [], annotations: [] };
        return {
            shapes: [{
                type: "rect",
                xref: "x",
                yref: "paper",
                x0: evento.inicio,
                x1: evento.finExclusivo,
                y0: 0,
                y1: 1,
                fillcolor: cssToken("--plot-warning"),
                opacity: 0.15,
                line: { width: 0 },
                layer: "below"
            }],
            annotations: [{
                x: evento.inicio,
                xref: "x",
                y: 1,
                yref: "paper",
                yshift: -8,
                xanchor: "left",
                text: "Evento (aprox.)",
                showarrow: false,
                font: { color: cssToken("--plot-ink"), size: 11 }
            }]
        };
    }

    async function drawChart(target, serie, title, zeroLine = false, evento = null) {
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
        const decoracion = decoracionEvento(evento);
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
            shapes: decoracion.shapes,
            annotations: decoracion.annotations,
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
            document.querySelector("#trend-explanation").textContent =
                `Datos insuficientes para estimar la tendencia. ${analisis.mensajeTendencia || ""}`.trim();
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

    function resetAnalysisResults() {
        [elements.seriesChart, elements.anomalyChart].forEach((chart, index) => {
            if (typeof window.Plotly !== "undefined" && chart.classList.contains("js-plotly-plot")) {
                window.Plotly.purge(chart);
            }
            const empty = document.createElement("p");
            empty.className = "chart-empty";
            empty.textContent = index === 0 ? "Cargando serie…" : "Cargando anomalías…";
            chart.replaceChildren(empty);
        });

        document.querySelector("#trend-title").textContent = "Calculando tendencia…";
        document.querySelector("#trend-slope").textContent = "—";
        document.querySelector("#trend-pvalue").textContent = "—";
        document.querySelector("#trend-interval").textContent = "—";
        document.querySelector("#significance-light").textContent = "Calculando…";
        document.querySelector("#significance-light").dataset.state = "unavailable";
        document.querySelector("#trend-explanation").textContent = "Esperando los resultados del nuevo periodo.";

        document.querySelector("#event-title").textContent = "Comparación histórica";
        document.querySelector("#event-value").textContent = "—";
        document.querySelector("#event-zscore").textContent = "—";
        document.querySelector("#event-percentile").textContent = "—";
        document.querySelector("#event-message").textContent = "Calculando comparación histórica…";
        elements.relations.replaceChildren("Calculando relaciones…");
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
        const evento = rangoEvento(demoActiva, analisis.serie);
        await Promise.all([
            drawChart(elements.seriesChart, analisis.serie, "Serie observada", false, evento),
            drawChart(elements.anomalyChart, analisis.anomalias, "Anomalía mensual", true, evento)
        ]);
        renderTrend(analisis);
        renderEvent(analisis);
        renderRelations(analisis);
        const observaciones = analisis.serie.puntos.filter(point => Number.isFinite(point.valor)).length;
        elements.message.textContent = observaciones === 0
            ? "Datos insuficientes: no hay observaciones válidas en el periodo seleccionado."
            : `${observaciones.toLocaleString("es-PE")} observaciones en el periodo; resultados calculados con la misma selección.`;
        setButtonState(elements.submit, "success", "Análisis actualizado");
    }

    function esErrorDeDatosInsuficientes(mensaje) {
        return /datos insuficientes|no hay datos|no hay observaciones|no se puede calcular/i.test(mensaje);
    }

    function mostrarDatosInsuficientes(mensaje) {
        elements.message.textContent = `Datos insuficientes para completar el análisis. ${mensaje}`;
        elements.message.dataset.state = "error";
        [elements.seriesChart, elements.anomalyChart].forEach(chart => {
            const empty = document.createElement("p");
            empty.className = "chart-empty";
            empty.textContent = "No hay observaciones suficientes para mostrar este gráfico.";
            chart.replaceChildren(empty);
        });
        setButtonState(elements.submit, "error", "Intentar de nuevo");
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
            if (esErrorDeDatosInsuficientes(error.message)) {
                mostrarDatosInsuficientes(error.message);
            } else {
                elements.message.textContent = error.message;
                elements.message.dataset.state = "error";
                setButtonState(elements.submit, "error", "Intentar de nuevo");
            }
            return false;
        }
    }

    async function aplicarPreset(id) {
        const preset = DEMOS.find(demo => demo.id === id);
        if (!preset) return;
        const variable = catalogo.find(item => item.id === preset.variableId);
        if (!variable) {
            elements.message.textContent = `La variable ${preset.variableId} no está disponible en el catálogo.`;
            elements.message.dataset.state = "error";
            return;
        }
        demoActiva = preset;
        elements.variable.value = variable.grupo;
        populateRegions(preset.variableId);
        elements.from.value = preset.desde;
        elements.to.value = preset.hasta;
        captureSelection();
        renderDemoGuide(preset);
        resetAnalysisResults();
        const menu = document.querySelector(".project-menu");
        menu.open = false;
        await analizar();
        elements.demoGuide.scrollIntoView({
            behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
            block: "start"
        });
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

    elements.variable.addEventListener("change", () => {
        quitarDemo();
        populateRegions();
    });
    elements.region.addEventListener("change", () => {
        quitarDemo();
        const variable = selectedVariable();
        if (!variable) return;
        estado.variable = variable.grupo;
        estado.region = variable.id;
        setPeriodFromVariable(variable);
        showVariableExplanation(variable);
    });
    [elements.from, elements.to].forEach(control => {
        control.addEventListener("input", () => {
            quitarDemo();
            captureSelection();
        });
        control.addEventListener("change", captureSelection);
    });
    elements.form.addEventListener("submit", event => {
        event.preventDefault();
        analizar();
    });
    elements.demoOptions.addEventListener("click", event => {
        const button = event.target.closest("[data-preset]");
        if (button) aplicarPreset(button.dataset.preset);
    });
    elements.exitDemo.addEventListener("click", quitarDemo);

    async function initialize() {
        setPlotlyStatus();
        renderDemoOptions();
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
