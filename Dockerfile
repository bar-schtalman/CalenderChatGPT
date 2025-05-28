FROM eclipse-temurin:17-jdk-jammy

COPY target/CalenderGPT*.jar /usr/src/CalenderGPT.jar
COPY src/main/resources/application.properties /opt/conf/application.properties

CMD ["java", "-jar", "/usr/src/CalenderGPT.jar", "--spring.config.location=file:/opt/conf/application.properties"]
