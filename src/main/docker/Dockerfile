
FROM  xxlabaza/server_jre
LABEL maintainer="Artem Labazin <xxlabaza@gmail.com>"

ENV JAVA_TOOL_OPTIONS="" \
    APP_CONFIG_FILE="/etc/config/configuration.yml"

ADD *.jar app.jar

ENTRYPOINT java -jar /app.jar "${APP_CONFIG_FILE}"
