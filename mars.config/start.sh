#!/bin/sh
#mvn package && java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8818 -jar target/config-1.0-SNAPSHOT.jar --logging.file=/tmp/boot.log --logging.level.mars.config=DEBUG --spring.profiles.active=dev
#java -jar target/config-1.0-SNAPSHOT.jar --logging.file=/tmp/boot.log --logging.level.mars.config=DEBUG --spring.profiles.active=test --server.port=9000
env=$1
process_id=`ps -ef|grep config-1.0-SNAPSHOT.jar|grep -v grep|awk '{print $2}'`
if [ -z "$process_id" ];then
    echo 'no process'
else
    echo "kill process $process_id"
    kill -9 $process_id
fi

BUILD_ID=dontKillMe nohup /opt/jdk7/bin/java -jar target/config-1.0-SNAPSHOT.jar --logging.file=/tmp/boot.log --logging.level.mars.config=DEBUG --spring.profiles.active=$env --server.port=9000 &