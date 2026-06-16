package com.fhir.server.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantUmlEncoderTest {

    private static final String SAMPLE = "@startuml\nclass Patient {\n  +name : HumanName\n}\n@enduml\n";

    /**
     * The encoder must produce something a PlantUML server can decode: raw DEFLATE wrapped in
     * PlantUML's base64 variant. Round-tripping (decode + inflate) must reproduce the input exactly.
     */
    @Test
    void encodeRoundTripsBackToSource() throws Exception {
        String encoded = PlantUmlEncoder.encode(SAMPLE);
        byte[] deflated = decode64(encoded);

        Inflater inflater = new Inflater(true); // nowrap=true: raw DEFLATE
        inflater.setInput(deflated);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!inflater.finished()) {
            int n = inflater.inflate(buf);
            if (n == 0) {
                break;
            }
            out.write(buf, 0, n);
        }
        inflater.end();

        assertEquals(SAMPLE, out.toString(StandardCharsets.UTF_8));
    }

    /** Output must only contain characters from PlantUML's alphabet: 0-9 A-Z a-z - _ */
    @Test
    void encodeUsesOnlyPlantUmlAlphabet() {
        String encoded = PlantUmlEncoder.encode(SAMPLE);
        assertFalse(encoded.isEmpty());
        assertTrue(encoded.matches("[0-9A-Za-z_-]+"), "unexpected characters in: " + encoded);
    }

    // Inverse of PlantUmlEncoder's encode64, for test verification only.
    private static byte[] decode64(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i += 4) {
            int c1 = decode6bit(s.charAt(i));
            int c2 = i + 1 < s.length() ? decode6bit(s.charAt(i + 1)) : 0;
            int c3 = i + 2 < s.length() ? decode6bit(s.charAt(i + 2)) : 0;
            int c4 = i + 3 < s.length() ? decode6bit(s.charAt(i + 3)) : 0;

            out.write(((c1 << 2) | (c2 >> 4)) & 0xFF);
            if (i + 2 < s.length()) {
                out.write(((c2 << 4) | (c3 >> 2)) & 0xFF);
            }
            if (i + 3 < s.length()) {
                out.write(((c3 << 6) | c4) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    private static int decode6bit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 36;
        }
        if (c == '-') {
            return 62;
        }
        if (c == '_') {
            return 63;
        }
        throw new IllegalArgumentException("Invalid char: " + c);
    }
}
