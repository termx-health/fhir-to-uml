package org.fhir.uml.generation;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Lives in its own class because {@code App}/{@code Config} use a one-shot static singleton
 * ({@code Config.fromArgs} ignores subsequent calls), so two {@code App.main} invocations cannot
 * share a JVM. {@code test.forkEvery = 1} gives each App-based test class a fresh JVM — matching
 * production, where every conversion is a fresh {@code java -jar} subprocess.
 */
public class AppLogicalModelTest {

    /**
     * A logical model (kind=logical, Parent=Element) with a generated snapshot but several
     * optional fields omitted — notably no {@code status} — must still render. Guards the
     * regression where the legend's {@code getStatus().getDisplay()} NPE'd, producing empty
     * output and PlantUML's "Welcome" placeholder.
     */
    @Test
    public void convertsLogicalModelWithoutStatus() throws Exception {
        Path input = Files.createTempFile("logical-input", ".json");
        Path output = Files.createTempFile("logical-output", ".puml");
        try {
            try (InputStream is = getClass().getResourceAsStream("/logical-no-status.json")) {
                assertNotNull("test resource logical-no-status.json must be present", is);
                Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
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
            assertTrue("missing TobaccoUse class", uml.contains("TobaccoUse"));
            assertTrue("missing logical-model field", uml.contains("patientCode"));
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }
}
