# Use an official lightweight Java 17 Runtime as the base image
FROM eclipse-temurin:17-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY build/libs/spring-music-1.0.jar app.jar

# Expose port 8080, which is the default port the Spring Boot app runs on
EXPOSE 8085

# Define the command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8085"]
