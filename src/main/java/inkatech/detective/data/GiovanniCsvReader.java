package inkatech.detective.data;

import inkatech.detective.config.VariableConfig;
import inkatech.detective.domain.PuntoTemporal;
import inkatech.detective.domain.SerieTemporal;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public SerieTemporal leer(Path path, VariableConfig variable) throws IOException {
        Objects.requireNonNull(path, "La ruta del CSV es obligatoria");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return leer(reader, variable);
        }
    }

    public SerieTemporal leer(InputStream input, VariableConfig variable) throws IOException {
        Objects.requireNonNull(input, "El contenido del CSV es obligatorio");
        return leer(new InputStreamReader(input, StandardCharsets.UTF_8), variable);
    }

    public SerieTemporal leer(Reader reader, VariableConfig variable) throws IOException {
        Objects.requireNonNull(reader, "El contenido del CSV es obligatorio");
        Objects.requireNonNull(variable, "La configuracion es obligatoria");
        String csv = extractDataBlock(reader);
        List<PuntoTemporal> points = parsePoints(csv);
        points.sort(Comparator.comparing(PuntoTemporal::fecha));
        return new SerieTemporal(variable.id(), variable.unidad(), points);
    }

    private static String extractDataBlock(Reader source) throws IOException {
        BufferedReader reader = source instanceof BufferedReader buffered ? buffered : new BufferedReader(source);
        StringBuilder csv = new StringBuilder();
        boolean headerFound = false;
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!headerFound && (lower.startsWith("time,") || lower.startsWith("\"time\",")
                    || lower.startsWith("date,") || lower.startsWith("\"date\","))) {
                headerFound = true;
            }
            if (headerFound && !trimmed.isEmpty()) csv.append(trimmed).append('\n');
        }
        if (!headerFound) throw new IllegalArgumentException("El CSV no contiene una cabecera time/date");
        return csv.toString();
    }

    private static List<PuntoTemporal> parsePoints(String csv) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true).build();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            List<PuntoTemporal> points = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() < 2) throw new IllegalArgumentException("Fila sin fecha y valor");
                points.add(new PuntoTemporal(parseDate(record.get(0)), parseValue(record.get(1))));
            }
            if (points.isEmpty()) throw new IllegalArgumentException("El CSV no contiene observaciones");
            return points;
        }
    }

    private static LocalDate parseDate(String raw) {
        String value = raw.trim();
        try { return LocalDate.parse(value); } catch (DateTimeParseException ignored) { }
        try { return YearMonth.parse(value).atDay(1); } catch (DateTimeParseException ignored) { }
        try { return OffsetDateTime.parse(value).toLocalDate(); } catch (DateTimeParseException ignored) { }
        try { return LocalDateTime.parse(value.replace(' ', 'T')).toLocalDate(); }
        catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Fecha invalida en CSV: " + raw, exception);
        }
    }

    private static double parseValue(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (List.of("", "na", "nan", "null", "--").contains(value)) return Double.NaN;
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed <= -9998 || Math.abs(parsed) >= 1e14) return Double.NaN;
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor invalido en CSV: " + raw, exception);
        }
    }
}
