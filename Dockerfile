FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
COPY config config
COPY docs docs
COPY examples examples
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/zeus-ibmi-extract-transform-0.2.0.jar /app/zeus-ibmi-extract-transform.jar
ENTRYPOINT ["java", "-jar", "/app/zeus-ibmi-extract-transform.jar"]