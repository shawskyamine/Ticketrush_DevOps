# ==========================================
# STAGE 1 : Build (Compilation avec Maven)
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# On copie d'abord le pom.xml seul (pour le cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# On copie le code source et on compile
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2 : Runtime (Image finale légère)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Création d'un utilisateur non-root (sécurité)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# On copie UNIQUEMENT le JAR depuis le stage 1
COPY --from=build /app/target/*.jar app.jar

# Documentation du port
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]