FROM alpine/java:21-jre

WORKDIR /app
COPY build/libs/TowerBow-1.0-SNAPSHOT-all.jar /app/towerbow.jar

EXPOSE 25565
EXPOSE 6000

ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar towerbow.jar"]