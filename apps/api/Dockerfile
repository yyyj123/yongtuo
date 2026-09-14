FROM eclipse-temurin:21.0.12_8-jdk-jammy AS build

RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw .mvn/wrapper/maven-wrapper.properties

COPY src ./src
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21.0.12_8-jre-jammy

RUN groupadd --system yongtuo \
    && useradd --system --gid yongtuo --home-dir /app --create-home yongtuo

WORKDIR /app
COPY --from=build /workspace/target/yongtuo-api-0.0.1-SNAPSHOT.jar app.jar

USER yongtuo

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
