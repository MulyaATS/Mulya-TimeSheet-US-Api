# -------- Build Stage --------
FROM openjdk:21-jdk-slim AS builder

RUN apt-get update && \
    apt-get install -y maven curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage --------
FROM openjdk:21-jdk-slim

WORKDIR /app

# Accept environment profile and port
ARG SPRING_PROFILES_ACTIVE=prod
ARG PORT=7072

ENV SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE
ENV PORT=$PORT

COPY --from=builder /app/target/employee.timesheet-0.0.1-SNAPSHOT.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$PORT -jar app.jar"]
