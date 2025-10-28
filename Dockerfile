FROM registry.ustpace.com/docker/openjdk:17-alpine

LABEL MAINTAINER="UST Global"

WORKDIR /app

# RUN apk --no-cache upgrade && apk add --no-cache wget bash tini

COPY ./target/*.jar /app/app.jar

USER ${APP_USER:-nobody}
ENTRYPOINT ["/entrypoint.sh"]
CMD ["tini", "--", "java", "-jar", "app.jar"]