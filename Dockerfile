# Menggunakan base image JDK 21 yang ringan (Alpine Linux)
FROM eclipse-temurin:21-jdk-alpine

# Menentukan direktori kerja di dalam kontainer
WORKDIR /app

# Menyalin file JAR hasil build ke dalam kontainer
COPY build/libs/*SNAPSHOT.jar app.jar

# Perintah untuk menjalankan aplikasi saat kontainer dimulai
ENTRYPOINT ["java", "-jar", "app.jar"]