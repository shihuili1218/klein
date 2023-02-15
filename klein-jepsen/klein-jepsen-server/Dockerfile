FROM java:8-jdk-alpine

# set environment
ENV KLEIN_ID="1" \
    KLEIN_PORT="8080" \
    KLEIN_IP="127.0.0.1" \
    KLEIN_MEMBERS="1:127.0.0.1:8080" \
    TIME_ZONE="Asia/Shanghai"

ADD target/klein-jepsen-server-shade.jar /klein/klein-server.jar
ADD deploy/start.sh /klein/start.sh
ADD deploy/stop.sh /klein/stop.sh

RUN chmod +x /klein/start.sh \
    && chmod +x /klein/stop.sh \
    && mkdir /config && touch /config/server.properties

EXPOSE 8080
CMD ["sh", "/klein/start.sh"]
