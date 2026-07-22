# ── ETAPA 1: BUILD FRONTEND ───────────────────────────────────────────────────
FROM node:20-alpine AS frontend-build

WORKDIR /fronted
COPY fronted/package*.json ./
RUN npm install
COPY fronted/ ./
RUN npm run build

# ── ETAPA 2: BUILD BACKEND ───────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS backend-build

WORKDIR /app

COPY backend/gradlew ./gradlew
COPY backend/gradle ./gradle
COPY backend/build.gradle ./build.gradle
COPY backend/settings.gradle ./settings.gradle
COPY backend/src ./src

# Copiar el dist del frontend donde Spring Boot lo espera
COPY --from=frontend-build /fronted/dist ./src/main/resources/static

RUN chmod +x gradlew
# -x buildFrontend -x npmInstall -x copyFrontend porque ya está copiado
RUN ./gradlew clean bootJar --no-daemon -x test -x buildFrontend -x npmInstall -x copyFrontend

# ── ETAPA 3: RUN ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xms128m", "-Xmx256m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]