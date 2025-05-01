# ---- Stage: Runtime image only ----
FROM eclipse-temurin:21-jdk-alpine

# Install graphviz and curl
RUN apk add --no-cache graphviz curl

WORKDIR /app

# Download PlantUML
RUN curl -L https://github.com/plantuml/plantuml/releases/download/v1.2025.2/plantuml-1.2025.2.jar -o plantuml.jar

# Copy prebuilt JARs from the host (we’ll build them outside Docker)
COPY server/build/libs/fhir-uml-converter.jar /app/server.jar
COPY converter/build/libs/fhir-uml-generation.jar /app/fhir-uml-generation.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
