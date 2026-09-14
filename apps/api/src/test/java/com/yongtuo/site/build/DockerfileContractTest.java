package com.yongtuo.site.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DockerfileContractTest {

    @Test
    void buildsWithVerifiedWrapperAndCurrentJava21Patch() throws IOException {
        String dockerfile = Files.readString(
                Path.of(System.getProperty("basedir"), "Dockerfile"));
        String wrapperProperties = Files.readString(Path.of(
                System.getProperty("basedir"),
                ".mvn",
                "wrapper",
                "maven-wrapper.properties"));

        assertThat(dockerfile)
                .contains("FROM eclipse-temurin:21.0.12_8-jdk-jammy AS build")
                .contains("apt-get install --yes --no-install-recommends unzip")
                .contains("COPY .mvn .mvn")
                .contains("COPY mvnw pom.xml ./")
                .contains("RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package")
                .contains("FROM eclipse-temurin:21.0.12_8-jre-jammy")
                .doesNotContain("FROM maven:")
                .doesNotContain("RUN mvn ");
        assertThat(wrapperProperties)
                .contains("apache-maven-3.9.16-bin.zip")
                .contains("distributionSha256Sum=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce");
    }
}
