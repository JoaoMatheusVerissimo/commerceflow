FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY config config
COPY services/gateway/pom.xml services/gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/customer-service/pom.xml services/customer-service/pom.xml
COPY services/catalog-service/pom.xml services/catalog-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml
RUN sh ./mvnw -B -ntp -pl services/inventory-service -am dependency:go-offline
COPY services/inventory-service/src services/inventory-service/src
RUN sh ./mvnw -B -ntp -pl services/inventory-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S commerceflow && adduser -S commerceflow -G commerceflow
USER commerceflow
COPY --from=build /workspace/services/inventory-service/target/inventory-service-*.jar /app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app.jar"]
