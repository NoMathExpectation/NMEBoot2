FROM amazoncorretto:11.0.24-alpine
COPY build/install/NMEBoot2 /app/
WORKDIR /app
VOLUME /app
ENTRYPOINT ["/app/bin/NMEBoot2"]