# Estágio 1: Build da aplicação com Maven
# Usamos uma imagem que já contém o Maven e o JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Copia o arquivo de configuração do Maven para aproveitar o cache de dependências
COPY pom.xml .

# Baixa as dependências do projeto
RUN mvn dependency:go-offline

# Copia todo o código-fonte da aplicação
COPY src ./src

# Executa o build do projeto. O plugin JAXB irá gerar as classes do XSD.
# -DskipTests pula a execução dos testes para agilizar o build.
RUN mvn clean package -DskipTests


# Estágio 2: Geração da imagem final de execução
# Usamos uma imagem JRE (Java Runtime Environment) que é menor que a JDK
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho
WORKDIR /app

# Copia o JAR gerado no estágio de build para a imagem final
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta em que a aplicação Spring Boot roda
EXPOSE 8080

# Define o comando para iniciar a aplicação quando o container for executado
ENTRYPOINT ["java", "-jar", "app.jar"]