# Thin runtime image for the order-service.
# The executable bootJar is built by Gradle (./gradlew :order-service:bootJar -> build/libs/app.jar)
# before this image is built; the CI publish workflow does exactly that.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 spring
COPY build/libs/app.jar app.jar
USER spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
