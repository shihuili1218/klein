FROM maven:3.6-openjdk-8
MAINTAINER jake@apache.org

ENV LEIN_ROOT true

#
# Jepsen dependencies
#
RUN apt-get -y -q update \
    && apt-get install -qy libjna-java \
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
    dos2unix

# RUN apt-get remove openjdk* -y && apt-get install -qy openjdk-8-jdk

RUN wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && \
    mv lein /usr/bin && \
    chmod +x /usr/bin/lein && \
    lein self-install

# without --dev flag up.sh copies jepsen to these subfolders
# with --dev flag they are empty until mounted
#COPY jepsen/jepsen /jepsen/jepsen/
#RUN if [ -f /jepsen/jepsen/project.clj ]; then cd /jepsen/jepsen && lein install; fi
#COPY jepsen /jepsen/

ADD ./bashrc /root/.bashrc
ADD ./init.sh /init.sh
RUN dos2unix /init.sh /root/.bashrc \
    && chmod +x /init.sh

CMD /init.sh
