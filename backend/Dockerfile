FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["sh", "-c", "java -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=20.0 -Dserver.port=${PORT:-8080} -jar app.jar"]