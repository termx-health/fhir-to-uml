# ---- Stage: Runtime image only ----
# PlantUML rendering is offloaded to a PlantUML HTTP server (set PLANT_UML_URL),
# so no graphviz / plantuml.jar is needed in the image.
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /app

# Copy prebuilt JARs from the host (we’ll build them outside Docker)
COPY server/build/libs/fhir-uml-converter.jar /app/server.jar
COPY converter/build/libs/fhir-uml-generation.jar /app/fhir-uml-generation.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
