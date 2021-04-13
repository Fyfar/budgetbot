FROM openjdk:11
COPY target/budgetbot-1.0.0.jar /app.jar
WORKDIR /
CMD ["java", "-jar", "app.jar"]