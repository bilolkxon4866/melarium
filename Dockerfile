# Build the React/Vite web application.
FROM node:22-bookworm-slim AS frontend-build
WORKDIR /workspace
COPY package.json package-lock.json ./
COPY frontend/package.json ./frontend/package.json
RUN npm ci --workspace frontend --include-workspace-root=false
COPY frontend ./frontend
RUN npm run build --workspace frontend

# Package the frontend assets into the Spring Boot application.
FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

# Single Railway web service: Spring Boot API + same-origin static frontend.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /workspace/target/melarium-api-0.1.0-SNAPSHOT.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
