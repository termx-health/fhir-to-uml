package com.fhir.server.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

/**
 * Encodes PlantUML source text into the path segment expected by a PlantUML
 * HTTP server (e.g. {@code <server>/png/<encoded>}).
 *
 * <p>The wire format is raw DEFLATE (zlib without the 2-byte header) followed by
 * PlantUML's custom base64 alphabet ({@code 0-9 A-Z a-z - _}). This mirrors the
 * encoder used across the TermX ecosystem (web-commons {@code deflate.ts}) so the
 * same PlantUML server renders text produced here identically.
 */
public final class PlantUmlEncoder {

    private PlantUmlEncoder() {
    }

    public static String encode(String text) {
        byte[] deflated = deflate(text.getBytes(StandardCharsets.UTF_8));
        return encode64(deflated);
    }

    private static byte[] deflate(byte[] data) {
        // nowrap=true → raw DEFLATE stream, which is what PlantUML servers expect.
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 3));
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static String encode64(byte[] data) {
        StringBuilder sb = new StringBuilder(4 * ((data.length + 2) / 3));
        for (int i = 0; i < data.length; i += 3) {
            int b1 = data[i] & 0xFF;
            int b2 = i + 1 < data.length ? data[i + 1] & 0xFF : 0;
            int b3 = i + 2 < data.length ? data[i + 2] & 0xFF : 0;
            append3bytes(sb, b1, b2, b3);
        }
        return sb.toString();
    }

    private static void append3bytes(StringBuilder sb, int b1, int b2, int b3) {
        sb.append(encode6bit((b1 >> 2) & 0x3F));
        sb.append(encode6bit(((b1 & 0x3) << 4 | (b2 >> 4)) & 0x3F));
        sb.append(encode6bit(((b2 & 0xF) << 2 | (b3 >> 6)) & 0x3F));
        sb.append(encode6bit(b3 & 0x3F));
    }

    private static char encode6bit(int b) {
        if (b < 10) {
            return (char) ('0' + b);
        }
        b -= 10;
        if (b < 26) {
            return (char) ('A' + b);
        }
        b -= 26;
        if (b < 26) {
            return (char) ('a' + b);
        }
        b -= 26;
        if (b == 0) {
            return '-';
        }
        if (b == 1) {
            return '_';
        }
        return '?';
    }
}
