package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalizadorEventoActualTest {

    private final AnalizadorEventoActual analizador = new AnalizadorEventoActual();

    @Test
    void comparaElPromedioEneroAgostoConLaDistribucionAnualDeLaBase() {
        List<PuntoTemporal> puntos = new ArrayList<>();
        for (int anio = 2002; anio <= 2025; anio++) {
            double valor = anio - 2002;
            agregarEneroAAgosto(puntos, anio, valor);
        }
        agregarEneroAAgosto(puntos, 2026, 24.0);

        AnalizadorEventoActual.Resultado resultado = analizador.analizar(
                new SerieTemporal("test", "u", puntos));

        assertEquals(24, resultado.numeroDeAniosBase());
        assertEquals(8, resultado.mesesValidos2026());
        assertEquals(100.0, resultado.percentil(), 1.0e-12);
        assertEquals(25.0, resultado.periodoRetornoAnios(), 1.0e-12);
    }

    private static void agregarEneroAAgosto(List<PuntoTemporal> puntos, int anio, double valor) {
        for (int mes = 1; mes <= 8; mes++) {
            puntos.add(new PuntoTemporal(LocalDate.of(anio, mes, 1), valor));
        }
    }
}
