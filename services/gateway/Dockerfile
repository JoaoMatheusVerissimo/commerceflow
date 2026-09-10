FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY config config
COPY services/gateway/pom.xml services/gateway/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/customer-service/pom.xml services/customer-service/pom.xml
RUN sh ./mvnw -B -ntp -pl services/gateway -am dependency:go-offline
COPY services/gateway/src services/gateway/src
RUN sh ./mvnw -B -ntp -pl services/gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S commerceflow && adduser -S commerceflow -G commerceflow
USER commerceflow
COPY --from=build /workspace/services/gateway/target/gateway-*.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
