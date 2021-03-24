FROM adoptopenjdk/openjdk15:ubi
RUN mkdir /data
COPY target/uploading-files-0.0.1-SNAPSHOT.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
EXPOSE 8080/tcp