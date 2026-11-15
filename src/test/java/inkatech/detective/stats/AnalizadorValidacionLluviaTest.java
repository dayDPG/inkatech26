package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalizadorValidacionLluviaTest {

    @Test
    void calculaSpearmanYSesgoLateMenosFinalEnMesesComunes() {
        AnalizadorValidacionLluvia analizador =
                new AnalizadorValidacionLluvia(new Correlacion());
        List<PuntoTemporal> latePuntos = new ArrayList<>();
        List<PuntoTemporal> finalPuntos = new ArrayList<>();
        YearMonth mes = YearMonth.of(2002, 8);
        for (int indice = 0; indice < 53; indice++) {
            YearMonth fecha = mes.plusMonths(indice);
            finalPuntos.add(new PuntoTemporal(fecha.atDay(1), indice));
            latePuntos.add(new PuntoTemporal(fecha.atDay(1), indice + 10.0));
        }
        SerieTemporal late = new SerieTemporal("late", "mm/mes", latePuntos);
        SerieTemporal finalMensual = new SerieTemporal("final", "mm/mes", finalPuntos);

        AnalizadorValidacionLluvia.Resultado resultado = analizador.analizar(late, finalMensual);

        assertEquals(53, resultado.numeroDeMeses());
        assertEquals(1.0, resultado.correlacion(), 1.0e-12);
        assertEquals(10.0, resultado.sesgoMedioLateMenosFinal(), 1.0e-12);
        assertEquals(LocalDate.of(2002, 8, 1), resultado.desde());
        assertEquals(LocalDate.of(2006, 12, 31), resultado.hasta());
    }
}
