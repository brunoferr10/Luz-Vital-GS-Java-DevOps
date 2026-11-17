package br.com.minddev.luzvital;

import org.springframework.web.bind.annotation.*;

@RestController
public class VersaoController {
  @GetMapping("/api/v1/versao")
  public Dtos.Versao version() {
    return new Dtos.Versao("1.0.0", "Luz Vital: energia para cuidar da vida.");
  }
}
