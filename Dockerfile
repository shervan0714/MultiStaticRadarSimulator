# Builds the simulator, runs the tests, and starts its command-line mode
# (a container has no display for the GUI).
#   docker build -t radar-sim .
#   docker run --rm radar-sim --load examples/default-scenario.properties --compare
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon test fatJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/dist/MultiStaticRadarSimulator.jar app.jar
COPY --from=build /src/examples examples
ENTRYPOINT ["java", "-jar", "app.jar", "--cli"]
