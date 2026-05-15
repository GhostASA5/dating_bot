FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -D spring
COPY --from=build /build/target/*.jar /app/app.jar
USER spring:spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
