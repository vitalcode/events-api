FROM openjdk:8
MAINTAINER vitalcode

ENV SBT_VERSION  0.13.8
RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

ADD . /data
WORKDIR /data

RUN echo "==> fetch all dependencies from repo..." && \
    sbt clean compile

EXPOSE 9000

ENTRYPOINT ["sbt"]
CMD ["run"]
