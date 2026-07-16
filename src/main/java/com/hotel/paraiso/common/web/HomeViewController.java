package com.hotel.paraiso.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeViewController {

    /** La raíz lleva al dashboard (pantalla principal de la operación). */
    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
