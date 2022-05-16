FROM openjdk:8
ARG JAR_FILE=target/*.jar
COPY  order-service.jar
#COPY ${JAR_FILE} order-service.jar
ENTRYPOINT [ "java", "-jar", "/order-service.jar" ]
EXPOSE 9004
