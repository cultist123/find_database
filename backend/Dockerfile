# 阶段1：构建
FROM maven:3.8-eclipse-temurin-11 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2：运行
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]