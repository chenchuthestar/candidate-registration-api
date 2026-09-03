# Multi-stage build: compile with Maven, run with a smaller Java runtime image.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Spring Boot HTTPS is exposed internally on 8443.
EXPOSE 8443

# Persistent resume storage is mounted at /app/uploads.
RUN mkdir -p /app/uploads/resumes

COPY --from=build /app/target/candidate-registration-backend-1.0.0.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
