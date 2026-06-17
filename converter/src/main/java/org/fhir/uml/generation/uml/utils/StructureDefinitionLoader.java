package org.fhir.uml.generation.uml.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.r4.model.StructureDefinition;

/**
 * Parses a {@code StructureDefinition} of any supported FHIR version (R4 / R4B / R5) and returns it
 * as the R4 model the rest of the converter is written against.
 *
 * <p>The version is read from the resource's {@code fhirVersion} field. R4 and R4B share the same
 * {@code StructureDefinition} wire format, so both are parsed directly with the R4 context; R5
 * changed enough to warrant a real conversion (parsed with the R5 context, then normalized to R4
 * via HAPI's structure convertors). Unknown/absent versions default to R4. This keeps the
 * generation pipeline single-version while accepting whatever the upstream FHIR toolchain (e.g.
 * chef/SUSHI) emits, instead of relying on the R4 parser leniently tolerating R5 payloads.
 */
public final class StructureDefinitionLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StructureDefinitionLoader() {
    }

    public static StructureDefinition loadAsR4(String json) {
        FhirVersionEnum version = detectVersion(json);
        if (version == FhirVersionEnum.R5) {
            org.hl7.fhir.r5.model.Resource r5 = (org.hl7.fhir.r5.model.Resource)
                    FhirContext.forR5().newJsonParser().parseResource(json);
            return (StructureDefinition) VersionConvertorFactory_40_50.convertResource(r5);
        }
        // R4 and R4B (and unknown) share the R4 StructureDefinition wire format.
        return (StructureDefinition) FhirContext.forR4().newJsonParser().parseResource(json);
    }

    /** Reads {@code fhirVersion} from the raw JSON; falls back to R4 when missing or unrecognized. */
    static FhirVersionEnum detectVersion(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode fhirVersion = node.get("fhirVersion");
            if (fhirVersion != null && !fhirVersion.asText().isBlank()) {
                FhirVersionEnum version = FhirVersionEnum.forVersionString(fhirVersion.asText());
                if (version != null) {
                    return version;
                }
            }
        } catch (Exception ignored) {
            // malformed or unreadable version — fall back to R4
        }
        return FhirVersionEnum.R4;
    }
}
