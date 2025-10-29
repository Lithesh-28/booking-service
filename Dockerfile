# Step 1: Use lightweight JDK base image
FROM openjdk:21-jdk-slim

# Step 2: Set working directory inside the container
WORKDIR /app

# Step 3: Copy the JAR file from target folder into container
COPY target/booking-service-0.0.1-SNAPSHOT.jar booking-service.jar

# Step 4: Expose the application port
EXPOSE 8084

# Step 5: Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "booking-service.jar"]
