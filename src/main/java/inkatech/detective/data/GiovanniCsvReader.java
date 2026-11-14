package inkatech.detective.data;

import inkatech.detective.config.VariableConfig;
import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class GiovanniCsvReader {

    private static final List<String> MISSING_VALUES = List.of("", "na", "nan", "null", "--");

    public SerieTemporal leer(Path path, VariableConfig variable) throws IOException {
        Objects.requireNonNull(path, "La ruta del CSV es obligatoria");
        Objects.requireNonNull(variable, "La configuracion de variable es obligatoria");

        List<String> csvLines = Files.readAllLines(path).stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.stripLeading().startsWith("#"))
                .toList();
        if (csvLines.size() < 2) {
            throw new IllegalArgumentException("El CSV de Giovanni no contiene datos");
        }

        List<PuntoTemporal> points = parsePoints(csvLines);
        points.sort(Comparator.comparing(PuntoTemporal::fecha));
        return new SerieTemporal(variable.id(), variable.unidad(), points);
    }

    private static List<PuntoTemporal> parsePoints(List<String> csvLines) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (CSVParser parser = format.parse(new StringReader(String.join("\n", csvLines)))) {
            List<PuntoTemporal> points = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() < 2) {
                    throw new IllegalArgumentException(
                            "Cada fila del CSV debe contener fecha y valor");
                }
                points.add(new PuntoTemporal(
                        parseDate(record.get(0)),
                        parseValue(record.get(1))));
            }

            if (points.isEmpty()) {
                throw new IllegalArgumentException("El CSV de Giovanni no contiene observaciones");
            }
            return points;
        }
    }

    private static LocalDate parseDate(String rawDate) {
        String value = rawDate.trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            // Giovanni may export monthly labels or timestamps depending on the product.
        }
        try {
            return YearMonth.parse(value).atDay(1);
        } catch (DateTimeParseException ignored) {
            // Try an ISO timestamp as the final supported representation.
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Fecha invalida en CSV: " + rawDate, exception);
        }
    }

    private static double parseValue(String rawValue) {
        String value = rawValue.trim();
        if (MISSING_VALUES.contains(value.toLowerCase(Locale.ROOT))) {
            return Double.NaN;
        }

        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) ? parsed : Double.NaN;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor invalido en CSV: " + rawValue, exception);
        }
    }
}
