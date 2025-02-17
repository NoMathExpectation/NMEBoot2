FROM amazoncorretto:11.0.24-alpine
RUN apk add tzdata ffmpeg
COPY build/install/core /app/
WORKDIR /app
VOLUME ["/app", "/swap"]
ENTRYPOINT ["/app/bin/core"]