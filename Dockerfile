FROM java:latest
WORKDIR /opt/docker
ADD opt /opt
RUN ["chown", "-R", "daemon:daemon", "."]
EXPOSE 9000
USER daemon
ENTRYPOINT ["bin/events-api", "-Dconfig.resource=docker.conf"]
CMD []
