# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy only pom first to leverage Docker layer caching
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy sources and build the application
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /app/target/minitasker-backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 9001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
