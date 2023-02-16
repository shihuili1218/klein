# See https://github.com/jgoerzen/docker-debian-base
# See https://hub.docker.com/r/jgoerzen/debian-base-standard
FROM jgoerzen/debian-base-standard:buster

# I think this is a bug--debian-base-setup crashes because policy-rc.d isn't
# present in this image, and if you create it, exim crashes... do we actually NEED this? Maybe not...
#RUN mkdir /usr/sbin/policy-rc.d
#RUN run-parts --exit-on-error --verbose /usr/local/debian-base-setup

# Basic system stuff
RUN apt-get update
RUN apt-get install -y apt-transport-https

# Install packages
RUN apt-get -qy update && \
    apt-get -qy install \
        dos2unix \
        openssh-server \
        pwgen

ENV KLEIN_ID="1" \
    KLEIN_PORT="8080" \
    KLEIN_IP="127.0.0.1" \
    KLEIN_MEMBERS="1:127.0.0.1:8080" \
    TIME_ZONE="Asia/Shanghai"

# When run, boot-debian-base will call this script, which does final
# per-db-node setup stuff.
ADD setup-jepsen.sh /usr/local/preinit/03-setup-jepsen
RUN chmod +x /usr/local/preinit/03-setup-jepsen

ADD ../../klein-jepsen-server/target/klein-jepsen-server-shade.jar /klein/klein-server.jar
ADD ../../klein-jepsen-server/deploy/start.sh /klein/start.sh
ADD ../../klein-jepsen-server/deploy/stop.sh /klein/stop.sh

# Configure SSHD
RUN sed -i "s/#PermitRootLogin prohibit-password/PermitRootLogin yes/g" /etc/ssh/sshd_config

# Enable SSH server
ENV DEBBASE_SSH enabled

RUN chmod +x /klein/start.sh \
    && chmod +x /klein/stop.sh \
    && mkdir /config && touch /config/server.properties

# Install Jepsen deps
RUN apt-get install -qy build-essential bzip2 curl dnsutils faketime iproute2 iptables iputils-ping libzip4 logrotate man man-db net-tools ntpdate psmisc python rsyslog sudo tar unzip vim wget ca-certificates openjdk-8-jdk

EXPOSE 22
EXPOSE 8080
CMD ["/usr/local/bin/boot-debian-base"]
