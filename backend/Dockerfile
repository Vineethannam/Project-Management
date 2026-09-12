FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
RUN useradd --create-home sphere
WORKDIR /app
COPY --from=build /app/target/projectsphere-api-0.1.0.jar app.jar
RUN mkdir -p data/uploads && chown -R sphere:sphere /app
USER sphere
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
