package org.fhir.uml.generation;

import org.fhir.uml.generation.uml.utils.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for FSH-authored logical models (kind=logical, Parent=Element) that ship a
 * generated snapshot but omit optional StructureDefinition fields — notably no {@code status}.
 * Previously the legend's {@code getStatus().getDisplay()} NPE'd after the classes were built;
 * App swallowed it (exit 0) and wrote nothing, so PlantUML rendered its "Welcome" placeholder.
 */
public class AppLogicalModelTest {

    /**
     * {@code Config} is a one-shot static singleton ({@code fromArgs} ignores repeat calls), so
     * reset it before each test to allow multiple {@code App.main} invocations in one JVM. In
     * production this never matters — each conversion is a fresh {@code java -jar} subprocess.
     */
    @Before
    public void resetConfigSingleton() throws Exception {
        Field instance = Config.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    public void convertsLogicalModelSnapshotView() throws Exception {
        String uml = render("snapshot", new String[]{});
        assertTrue("missing @startuml", uml.contains("@startuml"));
        assertTrue("missing @enduml", uml.contains("@enduml"));
        assertTrue("missing TobaccoUse class", uml.contains("TobaccoUse"));
        assertTrue("missing logical-model field", uml.contains("patientCode"));
    }

    /**
     * The differential view with hide_removed_objects=true and bindings/constraints off — the
     * exact flag set the modeler's UML tab sends.
     */
    @Test
    public void convertsLogicalModelDifferentialView() throws Exception {
        String uml = render("differential", new String[]{
                "--hide_removed_objects", "true",
                "--show_constraints", "false",
                "--show_bindings", "false",
                "--reduce_slice_classes", "false",
                "--hide_legend", "false"
        });
        assertTrue("missing @startuml", uml.contains("@startuml"));
        assertTrue("missing @enduml", uml.contains("@enduml"));
        assertTrue("missing TobaccoUse class", uml.contains("TobaccoUse"));
    }

    private String render(String view, String[] extraArgs) throws Exception {
        Path input = Files.createTempFile("logical-input", ".json");
        Path output = Files.createTempFile("logical-output", ".puml");
        try {
            try (InputStream is = getClass().getResourceAsStream("/logical-no-status.json")) {
                assertNotNull("test resource logical-no-status.json must be present", is);
                Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
            }
            String[] base = {
                    "--mode", "uml",
                    "--input", input.toAbsolutePath().toString(),
                    "--txt", output.toAbsolutePath().toString(),
                    "--view", view
            };
            String[] args = new String[base.length + extraArgs.length];
            System.arraycopy(base, 0, args, 0, base.length);
            System.arraycopy(extraArgs, 0, args, base.length, extraArgs.length);

            App.main(args);
            return Files.readString(output, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
        }
    }
}
