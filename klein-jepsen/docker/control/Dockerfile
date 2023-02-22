FROM debian:bullseye
MAINTAINER jake@apache.org

ENV LEIN_ROOT true

#
# Jepsen dependencies
#
RUN apt-get -y -q update && \
    apt-get install -qy openjdk-8-jdk \
    libjna-java \
    vim \
    emacs \
    git \
    htop \
    screen \
    pssh \
    curl \
    wget \
    gnuplot \
    graphviz \
    dos2unix \
    maven \

RUN wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && \
    mv lein /usr/bin && \
    chmod +x /usr/bin/lein && \
    lein self-install

# without --dev flag up.sh copies jepsen to these subfolders
# with --dev flag they are empty until mounted
COPY jepsen/jepsen /jepsen/jepsen/
RUN if [ -f /jepsen/jepsen/project.clj ]; then cd /jepsen/jepsen && lein install; fi
COPY jepsen /jepsen/

ADD klein-server.jar /jepsen/klein-server.jar
ADD ./bashrc /root/.bashrc
ADD ./init.sh /init.sh
RUN dos2unix /init.sh /root/.bashrc \
    && chmod +x /init.sh \
    && git clone https://github.com/shihuili1218/klein.git /jepsen/klein \
    && mvn install:install-file -DgroupId=com.ofcoder.klein.jepsen.server -DartifactId=klein-jepsen-server -Dversion=0.0.1 -Dpackaging=jar -Dfile=/jepsen/klein-server.jar

CMD /init.sh
