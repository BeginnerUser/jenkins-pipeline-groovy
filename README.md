# jenkins-pipeline-groovy

基于jenkins pipline的自动化构建参数化脚本

项目要求:jdk1.8+、maven管理项目jar包、springboot构建项目

先决条件Jenkins需先配置好jdk1.8+和maven的版本。

>resources
   >>jenkinsfile  文件需要放到项目根目录下,和java项目中pom的同级即可
   >>
   >>app.json     文件是配置的远程部署项目的服务器信息
   >>
   >>scripts      文件夹下是一些操作的shell脚本
>
>vars             
   >>main.groovy   文件是整个自动化的入口主体文件
