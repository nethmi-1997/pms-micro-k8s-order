FROM openjdk:8
ARG JAR_FILE=target/*.jar
COPY --from=build /home/app/target//*.jar order-service.jar
#COPY ${JAR_FILE} order-service.jar
ENTRYPOINT [ "java", "-jar", "/order-service.jar" ]
EXPOSE 9004
