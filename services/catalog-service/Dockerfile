FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY config config
COPY services/gateway/pom.xml services/gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/catalog-service/pom.xml services/catalog-service/pom.xml
RUN sh ./mvnw -B -ntp -pl services/catalog-service -am dependency:go-offline
COPY services/catalog-service/src services/catalog-service/src
RUN sh ./mvnw -B -ntp -pl services/catalog-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S commerceflow && adduser -S commerceflow -G commerceflow
USER commerceflow
COPY --from=build /workspace/services/catalog-service/target/catalog-service-*.jar /app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
