"use strict";

// T40: base y gráfico. T41: selectores de variable, región y periodo.
// T42 agrega la línea de tendencia y T43 el panel de resultado.

const API_BASE = "/api";

// Nombre legible de cada grupo del catálogo (si falta uno, se muestra el id).
const ETIQUETAS_GRUPO = {
    "sea-surface-temperature": "Temperatura del mar",
    "chlorophyll-a": "Clorofila-a",
    "precipitation": "Lluvia",
    "eastward-wind": "Viento zonal"
};

const elAviso = document.getElementById("aviso");
const selVariable = document.getElementById("selVariable");
const selRegion = document.getElementById("selRegion");
const selDesde = document.getElementById("selDesde");
const selHasta = document.getElementById("selHasta");
const btnAnalizar = document.getElementById("btnAnalizar");
const infoVariable = document.getElementById("infoVariable");

let catalogo = []; // lista de series (VariableConfig) que devuelve /api/variables

function mostrarAviso(texto) {
    elAviso.textContent = texto;
    elAviso.hidden = false;
}

function ocultarAviso() {
    elAviso.hidden = true;
}

async function pedirJson(url) {
    const respuesta = await fetch(url);
    if (!respuesta.ok) {
        let mensaje = "Error " + respuesta.status;
        try {
            const cuerpo = await respuesta.json();
            mensaje = cuerpo.mensaje || cuerpo.message || mensaje;
        } catch (e) {
            // la respuesta no era JSON: se deja el mensaje por defecto
        }
        throw new Error(mensaje);
    }
    return respuesta.json();
}

/**
 * Dibuja una serie de tiempo.
 * fechas: lista de textos "AAAA-MM-DD"; valores: lista de numeros o null.
 */
function graficarSerie(idDiv, fechas, valores, titulo, unidad) {
    const traza = {
        x: fechas,
        y: valores,
        type: "scatter",
        mode: "lines",
        name: titulo,
        connectgaps: false
    };
    const diseno = {
        margin: { t: 20, r: 20, b: 50, l: 60 },
        xaxis: { title: "Fecha" },
        yaxis: { title: unidad },
        paper_bgcolor: "rgba(0,0,0,0)",
        plot_bgcolor: "rgba(0,0,0,0)",
        font: { color: getComputedStyle(document.body).color }
    };
    return Plotly.newPlot(idDiv, [traza], diseno, { responsive: true, displaylogo: false });
}

// Jackson escribe los NaN como el texto "NaN": aquí se convierten en huecos (null).
function valorONull(valor) {
    return typeof valor === "number" && Number.isFinite(valor) ? valor : null;
}

function parrafo(texto) {
    const p = document.createElement("p");
    p.textContent = texto;
    return p;
}

function opcion(valor, texto) {
    const o = document.createElement("option");
    o.value = valor;
    o.textContent = texto;
    return o;
}

// ---------- Selectores (T41) ----------

function gruposDelCatalogo() {
    return [...new Set(catalogo.map(v => v.grupo))];
}

function seriesDelGrupo(grupo) {
    return catalogo.filter(v => v.grupo === grupo);
}

function etiquetaRegion(serie, hermanas) {
    const repetida = hermanas.filter(v => v.region.nombre === serie.region.nombre).length > 1;
    if (!repetida) {
        return serie.region.nombre;
    }
    return serie.region.nombre + (serie.resolucion === "DIARIA" ? " (diaria)" : " (mensual)");
}

function llenarVariables() {
    selVariable.replaceChildren(
        ...gruposDelCatalogo().map(g => opcion(g, ETIQUETAS_GRUPO[g] || g)));
    llenarRegiones();
}

function llenarRegiones() {
    const hermanas = seriesDelGrupo(selVariable.value);
    selRegion.replaceChildren(
        ...hermanas.map(v => opcion(v.id, etiquetaRegion(v, hermanas))));
    actualizarPeriodoEInfo();
}

function serieActual() {
    return catalogo.find(v => v.id === selRegion.value);
}

function aMes(fechaIso) {
    return fechaIso.slice(0, 7); // "2002-07-04" -> "2002-07"
}

function actualizarPeriodoEInfo() {
    const serie = serieActual();
    if (!serie) {
        return;
    }
    const minimo = aMes(serie.periodo.desde);
    const maximo = aMes(serie.periodo.hasta);

    selDesde.min = minimo;
    selDesde.max = maximo;
    selHasta.min = minimo;
    selHasta.max = maximo;
    selDesde.value = minimo;
    selHasta.value = maximo;

    const lineas = [
        parrafo(serie.definicion),
        parrafo("Datos disponibles: " + serie.periodo.desde + " a " + serie.periodo.hasta + "."),
        parrafo("Pista: " + serie.expectativaElNino)
    ];
    if (serie.notaDeSigno) {
        lineas.push(parrafo("Ojo: " + serie.notaDeSigno));
    }
    infoVariable.replaceChildren(...lineas);
}

const FORMATO_MES = /^\d{4}-(0[1-9]|1[0-2])$/;

function ultimoDiaDelMes(mes) {
    const anio = Number(mes.slice(0, 4));
    const numeroMes = Number(mes.slice(5, 7));
    return new Date(anio, numeroMes, 0).getDate();
}

/** Devuelve la selección lista para la API, o null (y muestra un aviso) si no es válida. */
function leerSeleccion() {
    const serie = serieActual();
    if (!serie) {
        mostrarAviso("Elige una variable y una región.");
        return null;
    }
    const desde = selDesde.value;
    const hasta = selHasta.value;

    if (!FORMATO_MES.test(desde) || !FORMATO_MES.test(hasta)) {
        mostrarAviso("Escribe las fechas como AAAA-MM, por ejemplo 2020-03.");
        return null;
    }
    if (desde > hasta) {
        mostrarAviso("La fecha inicial no puede ser posterior a la final.");
        return null;
    }
    if (desde < aMes(serie.periodo.desde) || hasta > aMes(serie.periodo.hasta)) {
        mostrarAviso("Elige fechas entre " + aMes(serie.periodo.desde) + " y " + aMes(serie.periodo.hasta) + ".");
        return null;
    }
    return {
        serie: serie,
        desde: desde + "-01",
        hasta: hasta + "-" + String(ultimoDiaDelMes(hasta)).padStart(2, "0")
    };
}

// ---------- Análisis ----------

async function alAnalizar() {
    ocultarAviso();
    const seleccion = leerSeleccion();
    if (!seleccion) {
        return;
    }

    const parametros = new URLSearchParams({
        var: seleccion.serie.id,
        desde: seleccion.desde,
        hasta: seleccion.hasta
    });

    try {
        const serie = await pedirJson(API_BASE + "/serie?" + parametros);
        const fechas = serie.puntos.map(p => p.fecha);
        const valores = serie.puntos.map(p => valorONull(p.valor));

        document.getElementById("tituloGrafico").textContent =
            seleccion.serie.nombre + " (" + seleccion.serie.region.nombre + ")";
        await graficarSerie("grafico", fechas, valores, seleccion.serie.nombre, seleccion.serie.unidad);
        document.getElementById("textoResumen").textContent =
            "Serie cargada. La tendencia y su significancia se agregan en T42 y T43.";
    } catch (error) {
        mostrarAviso(error.message);
    }
}

// ---------- Arranque ----------

function datosDeEjemplo() {
    const fechas = [];
    const valores = [];
    for (let i = 0; i < 36; i++) {
        const mes = String((i % 12) + 1).padStart(2, "0");
        const anio = 2023 + Math.floor(i / 12);
        fechas.push(anio + "-" + mes + "-01");
        valores.push(0.05 * i + Math.sin(i / 2));
    }
    return { fechas, valores };
}

async function cargarCatalogo() {
    try {
        catalogo = await pedirJson(API_BASE + "/variables");
    } catch (error) {
        mostrarAviso("No se pudo cargar el catálogo de variables (" + error.message + "). ¿Está corriendo el servidor?");
        return;
    }
    llenarVariables();
    [selVariable, selRegion, selDesde, selHasta, btnAnalizar].forEach(el => { el.disabled = false; });
}

document.addEventListener("DOMContentLoaded", () => {
    selVariable.addEventListener("change", llenarRegiones);
    selRegion.addEventListener("change", actualizarPeriodoEInfo);
    btnAnalizar.addEventListener("click", alAnalizar);

    if (typeof Plotly === "undefined") {
        mostrarAviso(
            "No se encontró js/plotly.min.js. Descárgalo y colócalo en src/main/resources/static/js/."
        );
    } else {
        // Gráfico de prueba: se reemplaza cuando el usuario pulsa Analizar.
        const ejemplo = datosDeEjemplo();
        document.getElementById("tituloGrafico").textContent = "Serie de ejemplo (datos de prueba)";
        graficarSerie("grafico", ejemplo.fechas, ejemplo.valores, "Ejemplo", "unidad");
    }

    cargarCatalogo();
});