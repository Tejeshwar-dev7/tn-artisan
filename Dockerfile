FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/artisanconnect.war /app/artisanconnect.war
EXPOSE 10000
ENTRYPOINT ["sh","-c","exec java -XX:ActiveProcessorCount=1 -XX:TieredStopAtLevel=1 -jar /app/artisanconnect.war --server.address=${SERVER_ADDRESS:-0.0.0.0} --server.port=${PORT:-10000}"]
