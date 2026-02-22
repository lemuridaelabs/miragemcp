FROM lemuridaelabs/openjdk-java24-jdk AS build

WORKDIR /home/appuser/app

# Copy Maven wrapper and pom.xml first for dependency caching
COPY --chown=appuser:appuser --chmod=755 mvnw mvnw
COPY --chown=appuser:appuser .mvn .mvn
COPY --chown=appuser:appuser pom.xml pom.xml

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY --chown=appuser:appuser src src
RUN ./mvnw package -DskipTests -B

FROM lemuridaelabs/openjdk-java24-jre

WORKDIR /app

COPY --from=build /home/appuser/app/target/honeymcp-*.jar app.jar

EXPOSE 8989

ENTRYPOINT ["java", "-jar", "app.jar"]
