FROM openjdk:8 as target
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} order-service.jar
ENTRYPOINT [ "java", "-jar", "/order-service.jar" ]
EXPOSE 9004
