# Build from the repository root:
#   docker build -f api-service/Dockerfile -t docmind-api .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml ./
COPY api-service ./api-service
COPY indexer-service ./indexer-service
RUN mvn -B package -pl api-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/api-service/target/api-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
