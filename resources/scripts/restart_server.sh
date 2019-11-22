#!/bin/bash
#1、停应用,2、压缩上一次构建包,3、删除上一次构建包,4、上传新的程序包,5、启应用

#服务器IP
ip=$1
#项目名
pjnm=$2
#项目部署路径
APP_NAME=$3

echo '$ip'
echo '$pjnm'
echo '$APP_NAME'
echo '更新并启动程序包-----------------------'
restart
#检查程序是否在运行
is_exist(){
  pid=`ps -ef|grep $APP_NAME|grep -v grep|awk '{print $2}'`
  #如果不存在返回1，存在返回0     
  if [ -z "${pid}" ] 
  then
   return 1
  else
    return 0
  fi
}

#停止方法
stop(){
  is_exist
  if [ $? -eq "0" ]
  then
    echo '停服务----------------------------------'
    kill -9 $pid
    sleep 1
    echo '压缩上一次程序包-------------------------'
    tar -czf ${pjnm}.backup.tar.gz ${APP_NAME}
    sleep 1
    echo '删除上一次程序包-------------------------'
    rm -rf ${APP_NAME}
    sleep 1
    echo '上传新程序包-----------------------------'
    scp -r target/*.jar ${pjnm}@${ip}:${APP_NAME}
  else
    echo "${APP_NAME} 程序包已停止-----------------"
  fi  
}

#启动方法
start(){
  is_exist
  if [ $? -eq 0 ]
  then
    echo "${APP_NAME} 服务正在运行中,对应进程号:pid=${pid}"
  else
    echo '启动服务-------------------------------------'
    nohup java -jar -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -Xms1024m -Xmx1024m -Xmn256m -Xss256k -XX:SurvivorRatio=8 -XX:+UseConcMarkSweepGC ${APP_NAME}  > ./log/app.log &
    sleep 5
    echo '服务启动成功---------------------------------'
  fi
}

#重启服务
restart(){
  stop
  sleep 2
  start
}
