package br.com.minddev.luzvital;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.Test;

class SchemaCompatibilityTest {
  @Test
  void ddlUsesSqlServerTypesAndMatchesCriticalColumns() throws Exception {
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .build();
    try {
      var metadata =
          new MetadataSources(registry)
              .addAnnotatedClass(Usuario.class)
              .addAnnotatedClass(Paciente.class)
              .addAnnotatedClass(Aparelho.class)
              .addAnnotatedClass(Ocorrencia.class)
              .getMetadataBuilder()
              .applyPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy())
              .build();
      String generated =
          String.join(
              ";\n", new SchemaCreatorImpl(registry).generateCreationCommands(metadata, false));
      Files.writeString(Path.of("target/hibernate-sqlserver-reference.sql"), generated);
      String ddl = Files.readString(Path.of("scripts/script-bd.sql")).toLowerCase();
      assertTrue(generated.contains("datetimeoffset(6)"));
      assertTrue(ddl.contains("datetimeoffset(6)"));
      for (String name :
          new String[] {
            "usuarios",
            "pacientes",
            "aparelhos",
            "ocorrencias",
            "autonomia_inicial",
            "potencia_watts",
            "evento_pendente",
            "evento_recebido",
            "plano_contingencia"
          }) {
        assertTrue(generated.contains(name), name);
        assertTrue(ddl.contains(name), name);
      }
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }
}
