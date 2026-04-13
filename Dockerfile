# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Step A: Copy ONLY the pom.xml first
COPY pom.xml .

# Step B: Download all dependencies (This layer gets cached!)
RUN mvn dependency:go-offline -B

# Step C: Copy your actual code
COPY src ./src
COPY data ./data

# Step D: Build the Fat JAR
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose the web port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]