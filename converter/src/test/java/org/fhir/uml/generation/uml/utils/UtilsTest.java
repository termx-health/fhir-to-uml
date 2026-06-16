package org.fhir.uml.generation.uml.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    /**
     * Choice-type element names (e.g. {@code value[x]} -> {@code valueDateTime}) are built by
     * capitalizing the type code's first letter while preserving the rest. This guards against a
     * regression to PlantUML's StringUtils.capitalize, which lowercased the tail and produced
     * FHIR-incorrect names like {@code valueDatetime} / {@code valueCodeableconcept}.
     */
    @Test
    public void capitalizePreservesTail() {
        assertEquals("DateTime", Utils.capitalize("dateTime"));
        assertEquals("CodeableConcept", Utils.capitalize("CodeableConcept"));
        assertEquals("Boolean", Utils.capitalize("boolean"));
        assertEquals("String", Utils.capitalize("string"));
        assertEquals("Quantity", Utils.capitalize("Quantity"));
    }
}
