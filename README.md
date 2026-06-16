# FHIR StructureDefinition → UML

Convert FHIR `StructureDefinition` resources into UML class diagrams (PlantUML), as text or
rendered PNG/SVG. Part of the [TermX](https://termx.org) ecosystem, it adds UML modelling to the
platform and helps bridge FHIR data models with traditional software-modelling tools.

> Try it in the sandbox: **[fhir-uml-converter.online](https://fhir-uml-converter.online)**

## How it works

The project is two JVM modules plus an external renderer:

```
            ┌──────────────┐   PlantUML text   ┌──────────────┐   PNG/SVG   ┌──────────────────┐
 FHIR JSON ─▶│   server     │──────────────────▶│  converter   │            │  PlantUML server │
   (HTTP)    │ (Spring Boot)│                   │ (HAPI FHIR)  │            │   (PLANT_UML_URL)│
            │              │◀──────────────────│   subprocess │            │                  │
            └──────┬───────┘   PlantUML text   └──────────────┘            └────────▲─────────┘
                   │                                                                 │
                   └──────────────── deflate+base64 encode, GET /png|/svg ───────────┘
```

- **`converter/`** — a CLI (HAPI FHIR) that parses a `StructureDefinition` and emits **PlantUML text**.
- **`server/`** — a Spring Boot REST API that runs the converter and, for image output, offloads
  rendering to a **PlantUML HTTP server** (it encodes the text and calls `PLANT_UML_URL`).

Because rendering is offloaded, the image contains **no Graphviz and no `plantuml.jar`** — the
TermX ecosystem already runs a PlantUML server, and any [PlantUML server](https://plantuml.com/server)
works.

## Quick start (Docker)

The published image is `ghcr.io/termx-health/fhir-uml-server`.

### Option A — use an existing PlantUML server

Configuration lives in [`fhir2uml.env`](fhir2uml.env); by default it points at the public TermX
demo PlantUML server.

```bash
docker compose up -d
```

### Option B — fully self-contained (bundled PlantUML server)

Runs fhir2uml together with its own PlantUML server, no external dependency:

```bash
docker compose -f docker-compose.example.yml up -d
```

### Try it

```bash
# Rendered PNG
curl -X POST http://localhost:8080/api/fhir2uml \
     -H 'Content-Type: image/png' \
     --data-binary @structuredefinition.json -o diagram.png

# PlantUML text
curl -X POST http://localhost:8080/api/fhir2uml \
     -H 'Content-Type: text/plain' \
     --data-binary @structuredefinition.json
```

## Configuration

The server is configured entirely through environment variables (see [`fhir2uml.env`](fhir2uml.env)):

| Variable           | Default                          | Description |
|--------------------|----------------------------------|-------------|
| `PLANT_UML_URL`    | `https://demo.termx.org/plantuml`| Base URL of the PlantUML HTTP server used to render PNG/SVG. |
| `SERVER_PORT`      | `8080`                           | Port the REST API listens on. Update the published port in compose to match. |
| `JDK_JAVA_OPTIONS` | *(unset)*                        | Optional JVM flags applied to the server **and** the converter subprocess (e.g. `-XX:MaxRAMPercentage=40`). |

## REST API

### `POST /api/fhir2uml`

Send a FHIR `StructureDefinition` JSON as the request body. The **output format is chosen by the
`Content-Type` request header**:

| `Content-Type`     | Response |
|--------------------|----------|
| `image/png`        | PNG diagram |
| `image/svg+xml`    | SVG diagram |
| anything else (e.g. `text/plain`) | PlantUML text |

Other headers:

| Header                    | Description |
|---------------------------|-------------|
| `Accept`                  | UML view via a `view` parameter: `application/json; view=snapshot` *(default)* or `view=differential`. |
| `Content-Disposition`     | `inline` *(default)* or `attachment; filename="diagram.png"` to force download. |
| `X-Hide-Removed-Objects`  | Exclude removed/unsupported elements. Default `true`. |
| `X-Show-Constraints`      | Include FHIR constraints. Default `true`. |
| `X-Show-Bindings`         | Show value-set bindings. Default `true`. |
| `X-Reduce-Slice-Classes`  | Collapse sliced elements into fewer UML classes. Default `false`. |
| `X-Hide-Legend`           | Hide the legend/notes. Default `false`. |

**Example**

```http
POST http://localhost:8080/api/fhir2uml
Accept: application/json; view=differential
Content-Type: image/svg+xml
Content-Disposition: attachment; filename="patient.svg"
X-Show-Constraints: false
```

## Building from source

**Requirements:** Java 25+ (Temurin/OpenJDK). The bundled Gradle wrapper handles Gradle 9.4.

### Converter

```bash
cd converter
./gradlew build          # -> build/libs/fhir-uml-generation.jar
```

Run it standalone (emits PlantUML text):

```bash
java -jar build/libs/fhir-uml-generation.jar \
  --input  path/to/structuredefinition.json \
  --txt    path/to/output.puml
```

| Parameter                 | Description |
|---------------------------|-------------|
| `--input`                 | Input file (FHIR `StructureDefinition` JSON, or UML `.txt` in `fhir` mode). |
| `--txt`                   | Path to write the generated PlantUML text. |
| `--output`                | *(optional)* Fallback output path for the text if `--txt` is omitted; the FHIR JSON in `fhir` mode. |
| `--mode uml\|fhir`        | Direction: `uml` (default) FHIR→UML, or `fhir` UML→FHIR. |
| `--view snapshot\|differential` | Which elements to include. Default `snapshot`. |
| `--hide_removed_objects`  | Exclude removed/unsupported elements. Default `true`. |
| `--show_constraints`      | Include constraints. Default `true`. |
| `--show_bindings`         | Show value-set bindings. Default `true`. |
| `--reduce_slice_classes`  | Collapse sliced elements. Default `false`. |
| `--hide_legend`           | Hide the legend/notes. Default `false`. |
| `--help`                  | Print usage and exit. |

To turn the text into an image, send it to any PlantUML server (this is what the REST server does).

### Server

```bash
cd server
./gradlew build          # -> build/libs/fhir-uml-converter.jar

# the server invokes the converter jar by name; place it alongside, or run via Docker
cp ../converter/build/libs/fhir-uml-generation.jar .

PLANT_UML_URL=https://demo.termx.org/plantuml \
  java -jar build/libs/fhir-uml-converter.jar
```

The server listens on `:8080` and expects `fhir-uml-generation.jar` in its working directory
(`converter.name.jar` in `application.properties`).

### Docker image

Two Dockerfiles are provided:

- **`Dockerfile`** — runtime-only; copies JARs you built on the host. Build the JARs first, then:
  ```bash
  docker build -t fhir-uml-server .
  ```
- **`Dockerfile.dev`** — multi-stage; builds both modules with Gradle inside the image:
  ```bash
  docker build -f Dockerfile.dev -t fhir-uml-server .
  ```

CI ([`.github/workflows/build.yml`](.github/workflows/build.yml)) builds the JARs and publishes a
multi-arch (amd64/arm64) image to GHCR on pushes to `main` and on `*.*.*` tags.

## License

MIT.
