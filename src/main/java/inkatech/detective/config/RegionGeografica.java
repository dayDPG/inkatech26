package inkatech.detective.config;

public record RegionGeografica(
        String nombre,
        double oeste,
        double sur,
        double este,
        double norte) {

    public RegionGeografica {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la region es obligatorio");
        }
        if (oeste < -180.0 || este > 180.0 || oeste >= este) {
            throw new IllegalArgumentException("Las longitudes de la region no son validas");
        }
        if (sur < -90.0 || norte > 90.0 || sur >= norte) {
            throw new IllegalArgumentException("Las latitudes de la region no son validas");
        }
    }
}
