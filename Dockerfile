# docker build -t ktor-ptv6-proxy:latest .
# docker run -p8093:8080 ktor-ptv6-proxy:latest

FROM gradle:8.10.2-jdk21-jammy AS builder

COPY src/ ./src/
COPY build.gradle.kts ./
COPY gradle.properties ./
COPY settings.gradle.kts ./
RUN gradle clean build --refresh-dependencies -x test
RUN mv ./build/libs/ktor-ptv6-proxy-all.jar /tmp/ktor-ptv6-proxy.jar

################
FROM eclipse-temurin:21.0.6_7-jre-jammy

RUN useradd -m ptv6_proxy
USER ptv6_proxy
WORKDIR /usr/src/myapp
COPY --from=builder /tmp/ktor-ptv6-proxy.jar ktor-ptv6-proxy.jar

HEALTHCHECK --interval=30s --timeout=30s --start-period=1s --retries=3 CMD [ $(curl --write-out '%{http_code}' --silent --output /dev/null  localhost:8080/Health) -eq "200" ]

EXPOSE 8080

ENTRYPOINT [ "java" ]
CMD [ "-jar", "ktor-ptv6-proxy.jar" ]
