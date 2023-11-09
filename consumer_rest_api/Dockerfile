FROM openjdk:17-slim-buster
WORKDIR /app
COPY ./build/libs/consumer_rest_api-0.2.0.jar /app
ENTRYPOINT ["java", "-jar", "consumer_rest_api-0.2.0.jar"]
EXPOSE 8081