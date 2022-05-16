FROM openjdk:8 
CMD ["java", "-jar", "shaded-artifact.jar"]
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} order-service.jar
ENTRYPOINT [ "java", "-jar", "/order-service.jar" ]
EXPOSE 9004
