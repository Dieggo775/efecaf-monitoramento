# Etapa 1: compila o projeto com Maven (não precisa ter Java/Maven no seu PC)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Etapa 2: imagem final, só com o Java de execução
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/loja.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
