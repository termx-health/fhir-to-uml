package com.fhir.server.service;

import com.fhir.server.util.Config;
import com.fhir.server.util.PlantUmlEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

@Service
public class ConverterService {

    private static final Logger log = LoggerFactory.getLogger(ConverterService.class);

    @Value("${converter.name.jar}")
    private String converterJarName;

    /**
     * Base URL of a PlantUML HTTP server (e.g. {@code https://demo.termx.org/plantuml}).
     * Rendering text → PNG/SVG is offloaded here instead of shelling out to a local
     * {@code plantuml.jar} + graphviz, matching how the rest of the TermX ecosystem
     * renders PlantUML.
     */
    @Value("${plantuml.server.url}")
    private String plantUmlServerUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String INPUT_FILE_BASENAME = "input";
    private static final String OUTPUT_TEXT_BASENAME = "output";

    public byte[] convertFhirToUml(String body, Config config) throws IOException, InterruptedException {
        log.info("Starting convertFhirToUml. mode={}, view={}, exportAs={}, contentType={}",
                config.getMode(), config.getView(), config.getContentType(), config.getContentType());
        log.debug("FHIR input body (truncated): {}", body.length() > 200
                ? body.substring(0, 200) + "..." : body);

        Path inputFile = Files.createTempFile(INPUT_FILE_BASENAME, ".json");
        Path outputTxt = Files.createTempFile(OUTPUT_TEXT_BASENAME, ".txt");

        Files.writeString(inputFile, body, StandardCharsets.UTF_8);
        log.info("Wrote FHIR input to temp file: {}", inputFile);

        try {
            // 1) Run main converter: FHIR StructureDefinition -> PlantUML text
            ProcessResult converterResult = runConverterJar(inputFile, outputTxt, config);
            log.info("Main converter finished with exitCode={}", converterResult.exitCode);

            if (converterResult.exitCode != 0) {
                log.error("Main converter jar failed. stderr:\n{}", converterResult.stderr);
                return buildFailedMessage(converterResult.exitCode, converterResult.stderr);
            }
            log.debug("Main converter stdout:\n{}", converterResult.stdout);

            String umlText = Files.readString(outputTxt, StandardCharsets.UTF_8);

            if (Objects.equals(config.getContentType(), MediaType.TEXT_PLAIN_VALUE)) {
                // Raw PlantUML text requested — return it as-is.
                log.info("Returning PlantUML text output ({} chars)", umlText.length());
                return umlText.getBytes(StandardCharsets.UTF_8);
            }

            // 2) Offload rendering to the PlantUML server.
            return renderViaPlantUmlServer(umlText, config);
        } finally {
            // Cleanup
            Files.deleteIfExists(inputFile);
            Files.deleteIfExists(outputTxt);
            log.debug("Cleaned up temp files: {}, {}", inputFile, outputTxt);
        }
    }

    public String convertUmlToFhir(String uml) {
        // not implemented
        return null;
    }

    private ProcessResult runConverterJar(Path inputFile, Path outputTxt, Config config)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", converterJarName,
                "--mode", config.getMode(),
                "--input", inputFile.toAbsolutePath().toString(),
                "--txt", outputTxt.toAbsolutePath().toString(),
                "--view", config.getView(),
                "--hide_removed_objects", String.valueOf(config.getHideRemovedObjects()),
                "--show_constraints", String.valueOf(config.getShowConstraints()),
                "--show_bindings", String.valueOf(config.getShowBindings()),
                "--reduce_slice_classes", String.valueOf(config.getReduceSliceClasses()),
                "--hide_legend", String.valueOf(config.getHideLegend())
        );

        log.debug("Running main converter jar with command: {}", pb.command());
        return runProcess(pb);
    }

    private byte[] renderViaPlantUmlServer(String umlText, Config config)
            throws IOException, InterruptedException {
        boolean isPng = Objects.equals(config.getContentType(), MediaType.IMAGE_PNG_VALUE);
        String format = isPng ? "png" : "svg";

        String base = plantUmlServerUrl.endsWith("/")
                ? plantUmlServerUrl.substring(0, plantUmlServerUrl.length() - 1)
                : plantUmlServerUrl;
        URI uri = URI.create(base + "/" + format + "/" + PlantUmlEncoder.encode(umlText));

        log.info("Requesting {} render from PlantUML server: {}", format, base);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            String detail = new String(response.body(), StandardCharsets.UTF_8);
            log.error("PlantUML server returned HTTP {} for {}: {}", response.statusCode(), uri, detail);
            return buildFailedMessage(response.statusCode(),
                    "PlantUML server (" + base + ") returned HTTP " + response.statusCode() + "\n" + detail);
        }
        log.info("PlantUML server returned {} bytes ({})", response.body().length, format);
        return response.body();
    }

    private ProcessResult runProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();

        try (InputStream is = process.getInputStream();
             InputStream es = process.getErrorStream()) {

            String stdout = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            return new ProcessResult(exitCode, stdout, stderr);
        }
    }

    private byte[] buildFailedMessage(int exitCode, String stderr) {
        String msg = "FAILED. exitCode=" + exitCode + "\n" + stderr;
        log.warn("Returning FAILED message: {}", msg);
        return msg.getBytes(StandardCharsets.UTF_8);
    }

    private static class ProcessResult {
        final int exitCode;
        final String stdout;
        final String stderr;
        ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
