# ---- Stage 1: Build with Gradle ----
FROM gradle:8.7-jdk21 AS build

# Set base working directory
WORKDIR /home/gradle/project

ENV GRADLE_OPTS="-Dorg.gradle.vfs.watch=false"

# Copy and build server
COPY server /home/gradle/project/server
WORKDIR /home/gradle/project/server
RUN gradle build --no-daemon

# Copy and build converter
COPY converter /home/gradle/project/converter
WORKDIR /home/gradle/project/converter
RUN gradle build --no-daemon

# ---- Stage 2: Runtime image ----
FROM eclipse-temurin:21-jdk-alpine

# Install graphviz and curl
RUN apk add --no-cache graphviz curl

WORKDIR /app

# Download PlantUML
RUN curl -L https://github.com/plantuml/plantuml/releases/download/v1.2025.2/plantuml-1.2025.2.jar -o plantuml.jar

# Copy the JARs from the build stage
COPY --from=build /home/gradle/project/server/build/libs/fhir-uml-converter.jar /app/server.jar
COPY --from=build /home/gradle/project/converter/build/libs/fhir-uml-generation.jar /app/fhir-uml-generation.jar

ENTRYPOINT ["java", "-jar", "/app/server.jar"]
