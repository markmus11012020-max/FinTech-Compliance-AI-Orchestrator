FROM huecker.io/library/maven:3.8.5-openjdk-17
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/compliance-orchestrator-1.0.0.jar"]