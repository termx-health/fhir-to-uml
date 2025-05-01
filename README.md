# FHIR StructureDefinition ↔ UML Transformation Tool

## Overview

This project focuses on developing tools and scripts that enable bidirectional, automated transformation between FHIR StructureDefinition resources and UML class diagrams. By bridging the gap between FHIR data models and traditional software modeling tools, the project aims to streamline healthcare systems development and foster interoperability. The resulting solution will also be integrated into the TermX platform, thereby enhancing its modeling capabilities.

### Key Objectives

1. **Bidirectional Conversion:**  
   - Automatically generate UML class diagrams (e.g., PlantUML) from FHIR StructureDefinitions.
   - Convert UML class diagrams back into valid FHIR StructureDefinitions.
   
2. **Integration with PlantUML:**  
   - Utilize PlantUML to produce readable and clear UML diagrams.
   
3. **TermX Platform Integration:**  
   - Add UML support to the TermX modeling environment, enabling users to view, edit, and maintain FHIR resources as UML diagrams.

### Significance

By enabling direct transformations between standard FHIR definitions and UML diagrams, this project:
- Simplifies the work of developers who rely on established modeling methodologies.
- Enhances interoperability and accelerates the development of healthcare IT systems.
- Expands TermX platform functionality, promoting open-source healthcare solution advancement.

## Requirements

- **Java 21 or above** (tested with OpenJDK)
- **Gradle** (latest version recommended)
- **HAPI FHIR libraries** (configured in `build.gradle`)
- **PlantUML**  
- **Graphviz** (required by PlantUML to generate diagrams)

### Checking and Installing Graphviz

**Check if Graphviz is installed:**
```bash
dot -V
```
If Graphviz is installed, this command should print a version number (e.g., `dot - graphviz version X.YZ`). If you get a "command not found" error, you need to install it.

**Install Graphviz:**

- On **Ubuntu/Debian**:
  ```bash
  sudo apt-get update
  sudo apt-get install graphviz
  ```

- On **Fedora/CentOS/RHEL**:
  ```bash
  sudo dnf install graphviz
  ```
  *or*
  ```bash
  sudo yum install graphviz
  ```

- On **macOS** (with Homebrew):
  ```bash
  brew install graphviz
  ```

- On **Windows**:  
  Download and install Graphviz from the official site: [https://graphviz.org/download/](https://graphviz.org/download/)

Once Graphviz is installed, `dot -V` should work, and PlantUML can generate UML diagrams.

## Building Converter

1. **Clone the repository:**
   ```bash
   git clone <repository_url>
   cd <project_directory>
   ```

2. **Go to the converter folder:**
   ```bash
   cd ./converter
   ```

3. **Build with Gradle Converter:**
   ```bash
   ./gradlew build
   ```
   This command will:
   - Download and install all required dependencies.
   - Compile the source code.
   - Run any included tests.
   - Produce a JAR file in `build/libs/`.

## Running the Converter from the Command Line

After a successful build, you can run the application as follows:

```bash
java -jar build/libs/fhir-uml-generation.jar --input path/to/structuredefinition.json --output path/to/output.png
```

**Command-line Parameters**

The `fhir-uml-generation.jar` supports a range of options to customize the transformation between FHIR and UML formats.

### Basic Usage

```bash
java -jar build/libs/fhir-uml-generation.jar \
    --input path/to/input.json \
    --output path/to/output.png \
    [--txt [output.txt]] \
    [--mode uml|fhir] \
    [--view snapshot|differential] \
    [--hide_removed_objects true|false] \
    [--show_constraints true|false] \
    [--show_bindings true|false] \
    [--reduce_slice_classes true|false] \
    [--hide_legend true|false] \
    [--help]
```

### Parameters

- `--input`  
  Path to the input file (FHIR StructureDefinition JSON or UML .txt depending on mode).
  
- `--output`  
  Path to the output file (PNG for UML mode, JSON for FHIR mode).
  
- `--txt` *(optional)*  
  Also generate the PlantUML text file. You can specify a custom filename or let it default.

- `--mode`  
  Conversion direction:  
  - `uml` (default): FHIR → UML  
  - `fhir`: UML → FHIR

- `--view`  
  What elements to include from the StructureDefinition:  
  - `snapshot` (default)  
  - `differential`

- `--hide_removed_objects`  
  Whether to exclude removed/unsupported FHIR elements. Default: `true`.

- `--show_constraints`  
  Whether to include constraints in the UML diagram. Default: `true`.

- `--show_bindings`  
  Whether to show value set bindings. Default: `true`.

- `--reduce_slice_classes`  
  Simplifies sliced elements into fewer UML classes. Default: `false`.

- `--hide_legend`  
  Whether to hide the legend and notes in the UML output. Default: `false`.

- `--help`  
  Prints full usage instructions and exits.

---

**Example (FHIR to UML):**
```bash
java -jar build/libs/fhir-uml-generation.jar \
  --input resources/example-structuredefinition.json \
  --output diagrams/generated-class-diagram.png
```

## Building and Running the Server (Optional)

The server provides a REST API that allows you to convert FHIR StructureDefinitions into UML diagrams via HTTP requests. To run the server, follow these steps:

### 1. Build the Converter First

Before building the server, make sure you’ve already built the **converter**:

```bash
cd converter
./gradlew build
```

Once built, copy the resulting JAR file to the server directory:

```bash
cp build/libs/fhir-uml-generation.jar ../server/
```

### 2. Download the PlantUML JAR

The server uses **PlantUML** to generate UML diagrams. You need to download the PlantUML JAR file:

```bash
cd ../server
curl -L https://github.com/plantuml/plantuml/releases/download/v1.2025.2/plantuml-1.2025.2.jar -o plantuml.jar
```

This will download and rename the file to `plantuml.jar` in the server folder.

### 3. Build the Server

Now build the server project:

```bash
./gradlew build
```

This will generate `fhir-uml-converter.jar` in `server/build/libs/`.

### 4. Run the Server

You can now run the server:

```bash
java -jar build/libs/fhir-uml-converter.jar
```

The server will start and listen on port `8080` by default.

---

## Using the API

Currently, the server provides a single endpoint:

### `POST /api/fhir2uml`

Converts a FHIR StructureDefinition to a UML diagram.

###  Headers

| Header                          | Description |
|----------------------------------|-------------|
| `Accept`                        | Specifies the response type and the UML view. Use:<br>• `application/json; view=snapshot` *(default)*<br>• `application/json; view=differential` |
| `Content-Type`                  | The type of input payload. Options:<br>• `application/json` — FHIR StructureDefinition (default)<br>• `image/png` — UML image in PlantUML<br>• `image/svg+xml` — SVG format input |
| `Content-Disposition`          | Controls how the response file is returned:<br>• `inline` *(default)* — displays in browser or client<br>• `attachment; filename="my-diagram.png"` — triggers file download |
| `X-Hide-Removed-Objects`       | Whether to exclude removed/unsupported elements. Default: `true`. |
| `X-Show-Constraints`           | Whether to include FHIR constraints in the UML diagram. Default: `true`. |
| `X-Show-Bindings`              | Whether to show value set bindings. Default: `true`. |
| `X-Reduce-Slice-Classes`       | Simplifies slice representation into fewer UML classes. Default: `false`. |
| `X-Hide-Legend`                | Whether to hide the UML diagram legend. Default: `false`. |

**Example request:**

```
POST http://localhost:8080/api/fhir2uml
Accept: application/json; view=snapshot
Content-Type: application/json
Content-Disposition: attachment; filename="patient-def.png"
X-Hide-Removed-Objects: false
X-Show-Constraints: false
X-Show-Bindings: false
X-Reduce-Slice-Classes: false
X-Hide-Legend: false
```

**Body:**  
Send a valid FHIR StructureDefinition JSON as the request body.

**Response:**  
Returns a UML class diagram (image/png) based on the input and headers.

## Building and Running with Docker (Optional)

This project includes a pre-configured Docker setup that automates the process of running the server with the converter and PlantUML.

###  1. Build the Docker Image

From the root of the project (where the `Dockerfile` is located), run:

```bash
docker build -t fhir-uml-server .
```

This command builds the image, including:

- Running Gradle builds for both the converter and server
- Downloading the required PlantUML JAR
- Packaging everything into a single runtime image

###  2. Run the Container with Docker Compose

If you have a `docker-compose.yml` in the root, you can start the container using:

```bash
docker compose up -d
```

This will:

- Start the container in detached mode
- Expose the server on `localhost:8080` (by default)
- Mount shared volumes (if defined)

###  Notes

- You don’t need to build the JARs manually — it’s all handled within the Docker image.

## License

This project is licensed under the **MIT** license.
