FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY shared-contracts shared-contracts
COPY auth-service auth-service
COPY product-service product-service
COPY cart-service cart-service
COPY order-service order-service
COPY inventory-service inventory-service
COPY payment-service payment-service
COPY api-gateway api-gateway
ARG SERVICE
RUN mvn -q -pl ${SERVICE} -am package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/${SERVICE}/target/*.jar app.jar
USER app
ENTRYPOINT ["java","-jar","app.jar"]
