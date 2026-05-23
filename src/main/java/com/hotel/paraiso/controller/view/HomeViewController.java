package com.hotel.paraiso.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeViewController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Hotel Paraíso");
        model.addAttribute("modules", buildModules());
        return "pages/home";
    }

    private List<Map<String, String>> buildModules() {
        return List.of(
                module("Tipos de Habitación", "Categorías base que determinan precio y capacidad", "/tipos-habitacion"),
                module("Habitaciones", "Gestión de las habitaciones físicas del hotel", "/habitaciones"),
                module("Clientes", "Huéspedes y datos de contacto", "/clientes"),
                module("Empleados", "Personal que gestiona las reservas", "/empleados"),
                module("Servicios", "Servicios adicionales del hotel", "/servicios"),
                module("Reservas", "Operación central del sistema", "/reservas"),
                module("Pagos", "Pagos asociados a las reservas", "/pagos"),
                module("Facturas", "Facturación de cada reserva", "/facturas")
        );
    }

    private Map<String, String> module(String name, String description, String path) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("path", path);
        return m;
    }
}
