package inkatech.detective.data;

import inkatech.detective.config.VariableConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

/**
 * Salta al bloque de metadatos, lee por índice y filtra valores faltantes.
 * Además acepta rutas relativas como ./Csv o del proyecto data/raw/.
 */
public final class GiovanniCsvReader {

    private static final List<String> VALORES_FALTANTES = List.of(
            "", "na", "nan", "null", "--",
            "-32767", "-32767.0",
            "-9999.9", "-9999.90", "-9999");

    private GiovanniCsvReader() {
    }

    public static SerieTemporal leer(Path ruta, VariableConfig variable) throws IOException {
        Objects.requireNonNull(variable, "La configuración de variable es obligatoria");

        Path rutaResolvida = resolverRuta(ruta, variable);
        if (!Files.isRegularFile(rutaResolvida)) {
            throw new IllegalArgumentException("No se encontró el CSV: " + rutaResolvida);
        }

        List<PuntoTemporal> puntos = parsearPuntos(rutaResolvida);
        puntos.sort(Comparator.comparing(PuntoTemporal::fecha));
        return new SerieTemporal(variable.id(), variable.unidad(), puntos);
    }

    private static Path resolverRuta(Path rutaUsuario, VariableConfig variable) {
        List<Path> rutasPosibles = new ArrayList<>();

        if (rutaUsuario != null) {
            rutasPosibles.add(rutaUsuario);
            if (!rutaUsuario.isAbsolute()) {
                rutasPosibles.add(Paths.get("").toAbsolutePath().resolve(rutaUsuario));
            }
        }

        if (variable != null && variable.archivo() != null) {
            Path rutaVariable = Paths.get(variable.archivo());
            rutasPosibles.add(rutaVariable);
            if (!rutaVariable.isAbsolute()) {
                rutasPosibles.add(Paths.get("").toAbsolutePath().resolve(rutaVariable));
            }
        }

        for (String base : List.of(".", "./Csv", "./data/raw", "data/raw", "src/main/resources/data")) {
            if (rutaUsuario != null && !rutaUsuario.isAbsolute()) {
                rutasPosibles.add(Paths.get(base).resolve(rutaUsuario));
            }
            if (variable != null && variable.archivo() != null) {
                rutasPosibles.add(Paths.get(base).resolve(variable.archivo()));
            }
        }

        for (Path ruta : rutasPosibles) {
            if (Files.isRegularFile(ruta)) {
                return ruta.toAbsolutePath().normalize();
            }
        }

        if (rutaUsuario != null) {
            return rutaUsuario.toAbsolutePath().normalize();
        }
        return Paths.get(variable.archivo()).toAbsolutePath().normalize();
    }

    private static List<PuntoTemporal> parsearPuntos(Path rutaArchivo) throws IOException {
        try (BufferedReader lector = Files.newBufferedReader(rutaArchivo, StandardCharsets.UTF_8)) {
            String linea;
            StringBuilder contenidoCsv = null;
            boolean cabeceraEncontrada = false;

            while ((linea = lector.readLine()) != null) {
                String lineaLimpia = linea.trim();
                if (lineaLimpia.isEmpty()) {
                    continue;
                }
                String normalizada = lineaLimpia.toLowerCase(Locale.ROOT);
                if (!cabeceraEncontrada && (normalizada.startsWith("time") || normalizada.startsWith("\"time\""))) {
                    cabeceraEncontrada = true;
                    contenidoCsv = new StringBuilder();
                    contenidoCsv.append(lineaLimpia).append('\n');
                    while ((linea = lector.readLine()) != null) {
                        String siguiente = linea.trim();
                        if (!siguiente.isEmpty()) {
                            contenidoCsv.append(siguiente).append('\n');
                        }
                    }
                    break;
                }
            }

            if (!cabeceraEncontrada || contenidoCsv == null || contenidoCsv.length() == 0) {
                throw new IllegalArgumentException("El CSV de Giovanni no contiene la cabecera 'time' válida");
            }

            return parsearContenidoCsv(contenidoCsv.toString());
        }
    }

    private static List<PuntoTemporal> parsearContenidoCsv(String contenidoCsv) throws IOException {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (CSVParser parser = formato.parse(new StringReader(contenidoCsv))) {
            List<PuntoTemporal> puntos = new ArrayList<>();
            for (CSVRecord registro : parser) {
                if (registro.size() < 2) {
                    throw new IllegalArgumentException(
                            "Cada fila de datos del CSV debe contener al menos fecha y valor");
                }

                LocalDate fecha = parsearFecha(registro.get(0));
                double valor = parsearValor(registro.get(1));
                puntos.add(new PuntoTemporal(fecha, valor));
            }

            if (puntos.isEmpty()) {
                throw new IllegalArgumentException("El CSV de Giovanni no contiene observaciones válidas");
            }
            return puntos;
        }
    }

    private static LocalDate parsearFecha(String fechaBruta) {
        if (fechaBruta == null || fechaBruta.isBlank()) {
            throw new IllegalArgumentException("La fecha en el CSV no puede estar vacía");
        }
        String valor = fechaBruta.trim();

        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ignorado) {
        }
        try {
            return YearMonth.parse(valor).atDay(1);
        } catch (DateTimeParseException ignorado) {
        }
        try {
            return OffsetDateTime.parse(valor).toLocalDate();
        } catch (DateTimeParseException ignorado) {
        }
        try {
            return LocalDateTime.parse(valor.replace(' ', 'T')).toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Fecha inválida en CSV: " + fechaBruta, exception);
        }
    }

    private static double parsearValor(String valorBruto) {
        if (valorBruto == null) {
            return Double.NaN;
        }
        String valor = valorBruto.trim();
        if (VALORES_FALTANTES.contains(valor.toLowerCase(Locale.ROOT))) {
            return Double.NaN;
        }

        try {
            double valorNumerico = Double.parseDouble(valor);
            if (valorNumerico <= -9998.0 || (valorNumerico <= -32766.0 && valorNumerico >= -32768.0)) {
                return Double.NaN;
            }
            return Double.isFinite(valorNumerico) ? valorNumerico : Double.NaN;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor inválido en CSV: " + valorBruto, exception);
        }
    }
}