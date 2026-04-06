
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
# Copiamos el pom y descargamos dependencias 
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copiamos solo el archivo JAR desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]