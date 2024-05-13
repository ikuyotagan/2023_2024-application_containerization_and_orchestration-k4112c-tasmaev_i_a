FROM gradle:8.4.0-jdk17 as builder
WORKDIR /src
COPY . .
RUN gradle assemble

FROM openjdk:17-jdk
WORKDIR /app
COPY --from=builder /src/build/libs/to-do-list-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]