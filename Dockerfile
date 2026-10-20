FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN useradd --system --create-home --uid 10001 app
COPY --from=build --chown=app:app /app/target/safari-1.0.0.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 8096
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
