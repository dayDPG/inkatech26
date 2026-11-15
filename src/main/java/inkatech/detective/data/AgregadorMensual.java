package inkatech.detective.data;

import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public final class AgregadorMensual {
    public SerieTemporal sumar(SerieTemporal diaria) {
        Map<YearMonth, Double> totals = new LinkedHashMap<>();
        diaria.puntos().stream().filter(point -> Double.isFinite(point.valor())).forEach(point ->
                totals.merge(YearMonth.from(point.fecha()), point.valor(), Double::sum));
        var points = totals.entrySet().stream()
                .map(entry -> new PuntoTemporal(entry.getKey().atDay(1), entry.getValue()))
                .toList();
        return new SerieTemporal(diaria.variableId(), "mm/mes", points);
    }
}
