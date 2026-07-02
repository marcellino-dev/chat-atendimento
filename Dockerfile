FROM openjdk:21-jdk-slim AS build

WORKDIR /app

# Copia o Maven wrapper e o pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Baixa as dependências
RUN ./mvnw dependency:go-offline

# Copia o código fonte
COPY src src

# Compila o projeto
RUN ./mvnw package -DskipTests

# Segunda etapa - imagem leve para execução
FROM openjdk:21-jre-slim

WORKDIR /app

# Copia o JAR da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta
EXPOSE 8080

# Comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]