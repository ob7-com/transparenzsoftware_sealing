# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -v >/dev/null 2>&1 || true
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/safesealing-0.9.2-runnable.jar /app/app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-cp","/app/app.jar","com.metabit.custom.safe.web.ServerMain"]


