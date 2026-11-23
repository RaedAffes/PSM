FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./ 
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist/frontend/browser ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p /var/lib/psm
COPY --from=backend-build /workspace/backend/target/psm-0.0.1-SNAPSHOT.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=docker
ENV PORT=2026
VOLUME ["/var/lib/psm"]
EXPOSE 2026
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
