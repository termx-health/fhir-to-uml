package org.fhir.uml.generation;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end exercise of the converter: parse a real R4 StructureDefinition with HAPI FHIR and
 * emit PlantUML text. Asserts the structural envelope and the FHIR-correct choice-type casing.
 */
public class AppIntegrationTest {

    @Test
    public void convertsPatientStructureDefinitionToPlantUmlText() throws Exception {
        Path input = Files.createTempFile("sd-input", ".json");
        Path output = Files.createTempFile("uml-output", ".puml");
        try {
            try (InputStream is = getClass().getResourceAsStream("/patient.profile.json")) {
                assertNotNull("test resource patient.profile.json must be present", is);
                Files.copy(is, input, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            App.main(new String[]{
                    "--mode", "uml",
                    "--input", input.toAbsolutePath().toString(),
                    "--txt", output.toAbsolutePath().toString(),
                    "--view", "snapshot"
            });

            String uml = Files.readString(output, StandardCharsets.UTF_8);
            assertTrue("missing @startuml", uml.contains("@startuml"));
            assertTrue("missing @enduml", uml.contains("@enduml"));
            assertTrue("missing Patient class", uml.contains("Patient"));

            // Choice-type expansion must keep FHIR casing.
            assertTrue("expected FHIR-correct choice name deceasedDateTime", uml.contains("deceasedDateTime"));
            assertFalse("must not regress to lowercased tail deceasedDatetime", uml.contains("deceasedDatetime"));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }
}
