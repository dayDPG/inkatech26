(() => {
    const status = document.querySelector("#plotly-status");
    if (!status) return;

    if (typeof window.Plotly === "undefined") {
        status.textContent = "Motor gráfico no disponible";
        status.dataset.state = "error";
        document.documentElement.dataset.plotly = "error";
        return;
    }

    status.textContent = `Plotly ${window.Plotly.version} · listo`;
    status.dataset.state = "ready";
    document.documentElement.dataset.plotly = "ready";
})();
