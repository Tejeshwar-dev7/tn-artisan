FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package
RUN mkdir /workspace/exploded && cd /workspace/exploded && jar xf ../target/artisanconnect.war
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/exploded/ /app/
EXPOSE 10000
ENTRYPOINT ["java","-Djava.net.preferIPv4Stack=true","-XX:ActiveProcessorCount=1","-XX:TieredStopAtLevel=1","-cp","/app","org.springframework.boot.loader.launch.WarLauncher"]
