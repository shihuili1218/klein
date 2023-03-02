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
        less \
        pwgen

# When run, boot-debian-base will call this script, which does final
# per-db-node setup stuff.
ADD setup-jepsen.sh /usr/local/preinit/03-setup-jepsen
RUN chmod +x /usr/local/preinit/03-setup-jepsen

# Configure SSHD
RUN sed -i "s/#PermitRootLogin prohibit-password/PermitRootLogin yes/g" /etc/ssh/sshd_config

# Enable SSH server
ENV DEBBASE_SSH enabled

RUN mkdir -p /klein/config

# Install Jepsen deps
RUN apt-get -y -q update && apt-get install -qy openjdk-8-jdk
RUN apt-get -y -q update && apt-get install -qy build-essential bzip2 curl dnsutils faketime iproute2 iptables iputils-ping libzip4 logrotate man man-db net-tools ntpdate psmisc python3 rsyslog sudo tar unzip vim wget ca-certificates
#    && apt-get update && apt-get install -yq software-properties-common && apt-get update && add-apt-repository ppa:openjdk-r/ppa && apt-get -y -q update && apt-get install -qy openjdk-8-jdk

RUN echo "root:123456" | chpasswd

EXPOSE 22
EXPOSE 1218
CMD ["/usr/local/preinit/03-setup-jepsen"]
