FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src

RUN javac src/Main.java -d out

CMD ["java", "-cp", "out", "Main"]

FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

CMD ["tail", "-f", "/dev/null"]