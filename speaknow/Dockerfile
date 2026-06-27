# =============================================
# Stage 1: Build the JAR using Maven
# =============================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml first for better Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# =============================================
# Stage 2: Run the JAR (slim runtime image)
# =============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose port (cloud providers override this via PORT env var)
EXPOSE 8080

# Run with prod profile (H2 in-memory, zero-config)
# MaxRAMPercentage=75 keeps memory under 512MB free-tier limit
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-jar", "app.jar"]
