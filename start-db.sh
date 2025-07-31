#!/bin/bash

EC2_USER=ubuntu
EC2_IP=3.39.23.140
PEM_PATH=./my-key.pem

echo "> EC2에서 MySQL 컨테이너 실행 확인"

ssh -i "$PEM_PATH" "$EC2_USER@$EC2_IP" <<EOF
  if [ "\$(docker ps -q -f name=my-mysql)" = "" ]; then
    echo "> MySQL 컨테이너 실행 중 아님. 새로 시작합니다."
    docker run --name my-mysql --rm\\
      -e MYSQL_ROOT_PASSWORD=1234 \\
      -e MYSQL_DATABASE=giftdb \\
      -e MYSQL_USER=giftuser \\
      -e MYSQL_PASSWORD=giftpass \\
      -p 3306:3306 \\
      -v mysql_data:/var/lib/mysql \\
      -d mysql:8
  else
    echo "> 이미 실행 중인 컨테이너가 있습니다."
  fi
EOF