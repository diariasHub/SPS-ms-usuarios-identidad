# syntax=docker/dockerfile:1.7

# ---------- Stage 1: build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copiar solo los archivos del wrapper y el POM primero para cachear la
# descarga de dependencias mientras el código cambia.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Ahora copiar el código y empaquetar.
COPY src ./src
RUN ./mvnw -B -ntp -DskipTests package \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime

# curl para healthcheck + usuario no-root.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --create-home --home-dir /home/spring spring
WORKDIR /app

# Capas en orden de menor a mayor frecuencia de cambio, para que Docker
# cachee las pesadas (dependencias) y solo recompile la última capa.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER spring:spring

# Perfil por defecto = dev. En prod basta con -e SPRING_PROFILES_ACTIVE=prod.
ENV SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
