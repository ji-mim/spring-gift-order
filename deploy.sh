#!/bin/bash

# EC2 정보 설정
EC2_USER=ubuntu
EC2_IP=3.39.23.140
PEM_PATH=./my-key.pem
REMOTE_DIR=/home/ubuntu

# 1. 로컬 빌드
echo "> 로컬에서 Gradle 빌드 시작"
./gradlew clean build

# 2. 빌드된 JAR 경로 찾기
JAR_PATH=$(find build/libs -name "*.jar" | head -n 1)
JAR_NAME=$(basename "$JAR_PATH")
echo "> 빌드 완료: $JAR_NAME"

# 3. EC2로 JAR 전송
echo "> EC2로 JAR 파일 전송 중"
scp -i "$PEM_PATH" "$JAR_PATH" "$EC2_USER@$EC2_IP:$REMOTE_DIR/$JAR_NAME"

# 4. EC2에서 실행 중인 프로세스 종료 후 재실행
echo "> EC2에서 애플리케이션 재시작 중"
ssh -i "$PEM_PATH" "$EC2_USER@$EC2_IP" <<EOF
  echo "> 현재 실행 중인 프로세스 확인"
  PID=\$(pgrep -f $JAR_NAME)

  if [ -z "\$PID" ]; then
    echo "> 실행 중인 애플리케이션 없음"
  else
    echo "> PID: \$PID 종료"
    kill -15 \$PID
    sleep 3
  fi

  echo "> 새로운 애플리케이션 실행"
  nohup java -jar $JAR_NAME > app.log 2>&1 &
EOF

echo "> 배포 완료"