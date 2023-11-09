FROM openjdk:17-slim-buster
WORKDIR /app
COPY ./build/libs/rest_api-0.2.0.jar /app
ENTRYPOINT ["java", "-jar", "rest_api-0.2.0.jar"]
EXPOSE 8080