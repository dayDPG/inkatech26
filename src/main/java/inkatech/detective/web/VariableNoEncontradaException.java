package inkatech.detective.web;

final class VariableNoEncontradaException extends RuntimeException {
    VariableNoEncontradaException(String id) { super("Variable no encontrada: " + id); }
}
