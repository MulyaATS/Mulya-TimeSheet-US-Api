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

RUN apt-get update && apt-get install -y ca-certificates && mkdir -p /etc/ssl/certs/custom

COPY nginx/ssl/mymulya.crt /etc/ssl/certs/custom/mymulya.crt

# Import certificate into Java truststore if exists
RUN if [ -f /etc/ssl/certs/custom/mymulya.crt ]; then \
      keytool -import -trustcacerts -alias mymulya_cert \
        -file /etc/ssl/certs/custom/mymulya.crt \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit -noprompt; \
    else \
      echo "Certificate file not found, skipping import"; \
    fi

ARG SPRING_PROFILES_ACTIVE=dev
ARG PORT=7071

ENV SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE
ENV PORT=$PORT

COPY --from=builder /app/target/employee.timesheet-0.0.1-SNAPSHOT.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$PORT -jar app.jar"]