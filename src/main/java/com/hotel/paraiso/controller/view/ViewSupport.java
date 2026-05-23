package com.hotel.paraiso.controller.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers estáticos para construir, desde los ViewControllers,
 * la estructura de columnas (tabla) y la estructura de campos (formulario)
 * que el template genérico Thymeleaf consume.
 */
public final class ViewSupport {

    private ViewSupport() {}

    /** Construye una columna: par {key, label} usado por la tabla genérica. */
    public static Map<String, String> column(String key, String label) {
        Map<String, String> col = new LinkedHashMap<>();
        col.put("key", key);
        col.put("label", label);
        return col;
    }

    /** Campo de formulario simple: type puede ser text, email, number, date, textarea, checkbox. */
    public static Map<String, Object> field(String key, String label, String type, boolean required) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", key);
        field.put("label", label);
        field.put("type", type);
        field.put("required", required);
        field.put("options", List.of());
        return field;
    }

    /** Campo de tipo select con sus opciones. */
    public static Map<String, Object> select(String key, String label, boolean required, List<Map<String, String>> options) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", key);
        field.put("label", label);
        field.put("type", "select");
        field.put("required", required);
        field.put("options", options);
        return field;
    }

    /** Campo select múltiple, vinculado a List&lt;Long&gt; / List&lt;String&gt; en el DTO. */
    public static Map<String, Object> multiselect(String key, String label, boolean required, List<Map<String, String>> options) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("key", key);
        field.put("label", label);
        field.put("type", "multiselect");
        field.put("required", required);
        field.put("options", options);
        return field;
    }

    /** Construye una opción {value, label} usada por los select. */
    public static Map<String, String> option(String value, String label) {
        Map<String, String> opt = new LinkedHashMap<>();
        opt.put("value", value);
        opt.put("label", label);
        return opt;
    }
}
