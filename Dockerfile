FROM amazoncorretto:11

WORKDIR /app

ADD build/libs/*-all.jar /app/app.jar
ADD configuration/dev.properties /app/config.properties

CMD java -noverify -jar /app/app.jar config.properties
