FROM amazoncorretto:11.0.24-alpine
RUN apk add tzdata
COPY build/install/scripting /app/
WORKDIR /app
VOLUME ["/app", "/swap"]
ENTRYPOINT ["/app/bin/scripting"]