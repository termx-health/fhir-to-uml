package org.fhir.uml.generation.uml.utils;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StructureDefinitionLoaderTest {

    @Test
    public void detectsVersionFromFhirVersionField() {
        assertEquals(FhirVersionEnum.R4, StructureDefinitionLoader.detectVersion("{\"fhirVersion\":\"4.0.1\"}"));
        assertEquals(FhirVersionEnum.R4B, StructureDefinitionLoader.detectVersion("{\"fhirVersion\":\"4.3.0\"}"));
        assertEquals(FhirVersionEnum.R5, StructureDefinitionLoader.detectVersion("{\"fhirVersion\":\"5.0.0\"}"));
    }

    @Test
    public void defaultsToR4WhenVersionMissingOrUnreadable() {
        assertEquals(FhirVersionEnum.R4, StructureDefinitionLoader.detectVersion("{\"resourceType\":\"StructureDefinition\"}"));
        assertEquals(FhirVersionEnum.R4, StructureDefinitionLoader.detectVersion("not json"));
    }

    /** R4B (4.3.0) input must normalize to an R4 StructureDefinition with its elements intact. */
    @Test
    public void loadsR4bLogicalModelAsR4() throws Exception {
        StructureDefinition sd = StructureDefinitionLoader.loadAsR4(read("/logical-no-status.json"));
        assertNotNull(sd);
        assertEquals("TobaccoUse", sd.getType());
        assertFalse("expected snapshot elements after conversion", sd.getSnapshot().getElement().isEmpty());
        assertTrue("expected the patientCode element to survive conversion",
                sd.getSnapshot().getElement().stream().anyMatch(e -> "TobaccoUse.patientCode".equals(e.getId())));
    }

    /** R5 (5.0.0) input must convert down to an R4 StructureDefinition with its elements intact. */
    @Test
    public void loadsR5LogicalModelAsR4() throws Exception {
        StructureDefinition sd = StructureDefinitionLoader.loadAsR4(read("/logical-r5.json"));
        assertNotNull(sd);
        assertFalse("expected snapshot elements after R5->R4 conversion", sd.getSnapshot().getElement().isEmpty());
        assertTrue("expected the systolic element to survive conversion",
                sd.getSnapshot().getElement().stream().anyMatch(e -> e.getId() != null && e.getId().endsWith(".systolic")));
    }

    private String read(String resource) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            assertNotNull("missing test resource " + resource, is);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
