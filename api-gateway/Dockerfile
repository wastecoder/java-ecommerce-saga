# Thin runtime image for the api-gateway service.
# The executable bootJar is built by Gradle (./gradlew :api-gateway:bootJar -> build/libs/app.jar)
# before this image is built; the CI publish workflow does exactly that.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY build/libs/app.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
