FROM eclipse-temurin:21-jdk-alpine@sha256:4fb80de7aeeb277ad949cfbe89b4f504e50bb34c57fd908c5825236473d71e986

WORKDIR /app

COPY build/libs/*SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]