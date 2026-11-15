package inkatech.detective.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import inkatech.detective.data.PuntoTemporal;
import inkatech.detective.data.SerieTemporal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorrelacionTest {

    private final Correlacion analizador = new Correlacion();

    @Test
    void alineaPorMesYUsaLaSerieAComoLiderDelRezago() {
        List<PuntoTemporal> puntosA = new ArrayList<>();
        List<PuntoTemporal> puntosB = new ArrayList<>();
        YearMonth inicio = YearMonth.of(2018, 1);
        puntosB.add(new PuntoTemporal(inicio.atDay(1), -1.0));
        for (int indice = 0; indice < 72; indice++) {
            YearMonth mes = inicio.plusMonths(indice);
            puntosA.add(new PuntoTemporal(mes.atDay(1), indice));
            puntosB.add(new PuntoTemporal(mes.plusMonths(1).atDay(1), indice));
        }
        SerieTemporal a = new SerieTemporal("a", "u", puntosA);
        SerieTemporal b = new SerieTemporal("b", "u", puntosB);

        ResultadoCorrelacion resultado = analizador.calcular(a, b, 1);

        assertEquals(72, resultado.numeroPares());
        assertEquals(1, resultado.rezagoMeses());
        assertEquals(0.9967421799986833, resultado.coeficiente(), 1.0e-12);
        assertEquals(2.772907831708384e-78, resultado.valorP(), 1.0e-88);
    }

    @Test
    void rechazaRezagosFueraDelRangoPermitido() {
        SerieTemporal serie = new SerieTemporal("serie", "u", List.of(
                punto(2020, 1, 1),
                punto(2020, 2, 2),
                punto(2020, 3, 3)));

        assertThrows(IllegalArgumentException.class, () -> analizador.calcular(serie, serie, 5));
    }

    @Test
    void excluyeMesesFaltantesYParcialesDeAmbasSeries() {
        List<PuntoTemporal> puntosA = new ArrayList<>();
        List<PuntoTemporal> puntosB = new ArrayList<>();
        YearMonth inicio = YearMonth.of(2018, 1);
        puntosB.add(new PuntoTemporal(inicio.atDay(1), -1.0));
        for (int indice = 0; indice < 72; indice++) {
            YearMonth mes = inicio.plusMonths(indice);
            double valorA = indice == 5 ? Double.NaN : indice;
            boolean parcialA = indice == 7;
            double valorB = indice == 20 ? Double.NaN : indice;
            boolean parcialB = indice == 21;
            puntosA.add(new PuntoTemporal(mes.atDay(1), valorA, parcialA));
            puntosB.add(new PuntoTemporal(mes.plusMonths(1).atDay(1), valorB, parcialB));
        }

        ResultadoCorrelacion resultado = analizador.calcular(
                new SerieTemporal("a", "u", puntosA),
                new SerieTemporal("b", "u", puntosB),
                1);

        assertEquals(68, resultado.numeroPares());
        assertTrue(Double.isFinite(resultado.coeficiente()));
        assertTrue(resultado.valorP() >= 0.0 && resultado.valorP() <= 1.0);
    }

    private static PuntoTemporal punto(int anio, int mes, double valor) {
        return new PuntoTemporal(LocalDate.of(anio, mes, 1), valor);
    }
}
