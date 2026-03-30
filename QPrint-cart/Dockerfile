FROM maven:4.0.0-rc-5-eclipse-temurin-25 AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY QPrint-gateway/pom.xml QPrint-gateway/pom.xml
COPY QPrint-auth/pom.xml QPrint-auth/pom.xml
COPY QPrint-objects/pom.xml QPrint-objects/pom.xml
COPY QPrint-cart/pom.xml QPrint-cart/pom.xml
COPY QPrint-checkout/pom.xml QPrint-checkout/pom.xml
COPY QPrint-orders/pom.xml QPrint-orders/pom.xml
COPY QPrint-otp/pom.xml QPrint-otp/pom.xml
COPY QPrint-transactions/pom.xml QPrint-transactions/pom.xml
COPY QPrint-shops/pom.xml QPrint-shops/pom.xml
RUN mvn -pl QPrint-cart -am dependency:go-offline
COPY . .
RUN mvn -pl QPrint-cart -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /workspace/QPrint-cart/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
