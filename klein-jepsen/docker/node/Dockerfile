# See https://github.com/jgoerzen/docker-debian-base
# See https://hub.docker.com/r/jgoerzen/debian-base-standard
FROM ubuntu:latest

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

ADD klein-server.jar /klein/klein-server.jar
ADD start.sh /klein/start.sh
ADD stop.sh /klein/stop.sh

# Configure SSHD
RUN sed -i "s/#PermitRootLogin prohibit-password/PermitRootLogin yes/g" /etc/ssh/sshd_config

# Enable SSH server
ENV DEBBASE_SSH enabled

RUN chmod +x /klein/start.sh \
    && chmod +x /klein/stop.sh \
    && mkdir /config && touch /config/server.properties

# Install Jepsen deps
RUN apt-get -y -q update && apt-get install -qy openjdk-8-jdk
RUN apt-get -y -q update && apt-get install -qy build-essential bzip2 curl dnsutils faketime iproute2 iptables iputils-ping libzip4 logrotate man man-db net-tools ntpdate psmisc python3 rsyslog sudo tar unzip vim wget ca-certificates
#    && apt-get update && apt-get install -yq software-properties-common && apt-get update && add-apt-repository ppa:openjdk-r/ppa && apt-get -y -q update && apt-get install -qy openjdk-8-jdk


EXPOSE 22
EXPOSE 8080
CMD ["/usr/local/preinit/03-setup-jepsen"]
