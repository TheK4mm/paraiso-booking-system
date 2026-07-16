package com.hotel.paraiso.common.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

/**
 * Exportación CSV (RFC 4180) con BOM UTF-8 para compatibilidad con Excel.
 */
public final class CsvExporter {

    private static final String BOM = "﻿";

    private CsvExporter() {
    }

    public static <T> ResponseEntity<byte[]> exportar(String nombreArchivo,
                                                      List<String> cabeceras,
                                                      List<T> filas,
                                                      Function<T, List<Object>> extractor) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(linea(cabeceras.stream().map(Object.class::cast).toList()));
        filas.forEach(fila -> sb.append(linea(extractor.apply(fila))));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String linea(List<Object> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(';'); // separador amigable con Excel en locales es-*
            }
            sb.append(escapar(valores.get(i)));
        }
        return sb.append("\r\n").toString();
    }

    private static String escapar(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = valor.toString();
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return '"' + texto.replace("\"", "\"\"") + '"';
        }
        return texto;
    }
}
