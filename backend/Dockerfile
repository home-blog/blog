# 서버(Spring Boot) 컨테이너. 배포용: docker-compose.deploy.yml이 부른다
# 1단계: Maven으로 jar 만들기 (테스트는 CI에서 이미 돈다)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/*.jar /app.jar

# 2단계: 실행에 필요한 JRE만 담는다. 관리자 권한 없이 실행
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 myblog
USER myblog
COPY --from=build /app.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod TZ=Asia/Seoul
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
