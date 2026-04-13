# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Step A: Copy ONLY the pom.xml first to cache dependencies
COPY pom.xml .

# Step B: Download all dependencies (This layer is cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Step C: Copy your source code (We skip copying 'data' folder for cloud safety)
COPY src ./src

# Step D: Build the Fat JAR
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Render usually uses port 8080 or 10000. 
EXPOSE 8080

# CRITICAL: Memory flags for Render Free Tier (512MB RAM)
# -Xmx300m: Limits the heap to 300MB so the container doesn't crash.
# -Xss512k: Reduces memory per thread to save space.
ENTRYPOINT ["java", "-Xmx300m", "-Xss512k", "-jar", "app.jar"]