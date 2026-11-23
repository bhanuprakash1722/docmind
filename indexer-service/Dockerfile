# Build from the repository root:
#   docker build -f indexer-service/Dockerfile -t docmind-indexer .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY api-service ./api-service
COPY indexer-service ./indexer-service
RUN mvn -B package -pl indexer-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/indexer-service/target/indexer-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
