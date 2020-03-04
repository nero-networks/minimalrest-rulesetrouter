FROM openjdk:8-jdk-alpine

COPY build/libs/*.jar app.jar

RUN adduser -s /sbin/nologin -D -u 1000 ruleset-router

USER ruleset-router
WORKDIR /home/ruleset-router

ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]
