# Stage 1: Build stage using Gradle and JDK 17
FROM gradle:8.4.0-jdk17 AS build

# Set working directory
WORKDIR /karbitrage

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties /karbitrage/
COPY gradle /karbitrage/gradle

# Copy source code
COPY src /karbitrage/src

# Build application
RUN gradle clean build --no-daemon

# Stage 2: Production stage using Amazon Corretto 17
FROM amazoncorretto:17

# Set working directory
WORKDIR /karbitrage

# Copy built application from build stage
COPY --from=build /karbitrage/build/libs/karbitrage-all.jar /karbitrage/karbitrage.jar

# Expose port
EXPOSE 8080

# Command to run the application
CMD ["java","-Denv=prod", "-jar", "karbitrage.jar"]