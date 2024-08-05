FROM amazoncorretto:11.0.24-alpine
RUN apk add tzdata ffmpeg
COPY build/install/NMEBoot2 /app/
WORKDIR /app
VOLUME /app
ENTRYPOINT ["/app/bin/NMEBoot2"]