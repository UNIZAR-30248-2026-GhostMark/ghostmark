FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
COPY src/ ./src/
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
