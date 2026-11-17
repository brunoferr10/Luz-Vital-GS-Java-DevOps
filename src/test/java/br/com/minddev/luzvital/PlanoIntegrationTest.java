package br.com.minddev.luzvital;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanoIntegrationTest {
  static HttpServer server;
  static volatile int responseStatus = 200;
  static volatile String captured = "";
  static volatile String output = "";

  static {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/chat/completions",
          exchange -> {
            captured = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String body =
                new ObjectMapper()
                    .writeValueAsString(
                        Map.of(
                            "id",
                            "test-chat",
                            "object",
                            "chat.completion",
                            "created",
                            1700000000,
                            "model",
                            "test-model",
                            "choices",
                            List.of(
                                Map.of(
                                    "index",
                                    0,
                                    "message",
                                    Map.of("role", "assistant", "content", output),
                                    "finish_reason",
                                    "stop"))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      server.start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("app.ai-enabled", () -> true);
    r.add("spring.ai.openai.base-url", () -> "http://127.0.0.1:" + server.getAddress().getPort());
    r.add("spring.ai.openai.chat.completions-path", () -> "/v1/chat/completions");
    r.add("spring.ai.openai.api-key", () -> "test-key");
    r.add("spring.ai.retry.max-attempts", () -> 1);
  }

  @Autowired MockMvc mvc;
  @Autowired AuthService auth;
  @Autowired ObjectMapper mapper;
  @Autowired PacienteRepository patients;
  @Autowired AparelhoRepository devices;
  @Autowired OcorrenciaRepository incidents;
  @Autowired UsuarioRepository users;
  Long pid;
  String token;

  @BeforeEach
  void setup() throws Exception {
    incidents.deleteAll();
    devices.deleteAll();
    patients.deleteAll();
    users.deleteAll();
    responseStatus = 200;
    captured = "";
    output =
        mapper.writeValueAsString(
            new Dtos.Plano(
                "Verifique o plano aprovado.",
                "Acione os contatos previstos.",
                "Mantenha contatos acessiveis.",
                "Rascunho"));
    var u =
        auth.register(new Dtos.Registro("Nome Sigiloso", "sigilo@example.com", "SenhaTeste123!"));
    token = auth.login(new Dtos.Login(u.email(), "SenhaTeste123!")).accessToken();
    var p = new Paciente();
    p.nome = "Pessoa Sigilosa";
    p.endereco = "Endereco Sigiloso";
    p.telefone = "11998887777";
    p.cuidador = users.findById(u.id()).orElseThrow();
    pid = patients.save(p).id;
    var d = new Aparelho();
    d.paciente = p;
    d.modelo = "Modelo sigiloso";
    d.tipo = Aparelho.Tipo.VENTILADOR;
    d.potenciaWatts = 100;
    d.autonomiaMinutos = 60;
    devices.save(d);
  }

  @Test
  void springAiGeneratesPersistsAndOmitsPersonalData() throws Exception {
    mvc.perform(
            post("/api/v1/pacientes/" + pid + "/plano").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aviso").value(org.hamcrest.Matchers.containsString("192")));
    assertTrue(captured.contains("VENTILADOR"));
    assertFalse(captured.contains("Pessoa Sigilosa"));
    assertFalse(captured.contains("Endereco Sigiloso"));
    assertFalse(captured.contains("11998887777"));
    assertFalse(captured.contains("sigilo@example.com"));
    assertTrue(patients.findById(pid).orElseThrow().plano.contains("primeirasAcoes"));
  }

  @Test
  void invalidAiOutputDoesNotOverwritePreviousPlan() throws Exception {
    var p = patients.findById(pid).orElseThrow();
    p.plano = "plano anterior revisado";
    patients.save(p);
    output = "{}";
    mvc.perform(
            post("/api/v1/pacientes/" + pid + "/plano").header("Authorization", "Bearer " + token))
        .andExpect(status().isServiceUnavailable());
    assertEquals("plano anterior revisado", patients.findById(pid).orElseThrow().plano);
  }

  @Test
  void providerFailureReturns503() throws Exception {
    responseStatus = 503;
    mvc.perform(
            post("/api/v1/pacientes/" + pid + "/plano").header("Authorization", "Bearer " + token))
        .andExpect(status().isServiceUnavailable());
  }
}
