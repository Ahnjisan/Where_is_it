# 1단계: Gradle로 실행용 jar를 만든다(테스트는 DB가 필요해서 제외된 bootJar만 실행).
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
# Windows에서 받은 파일은 실행 권한이 빠질 수 있어 다시 준다.
RUN chmod +x gradlew
# Gradle 캐시를 빌드 사이에 재사용해 두 번째 빌드부터 빨라진다.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon

# 2단계: 실행에 필요한 JRE와 jar만 담는다.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
