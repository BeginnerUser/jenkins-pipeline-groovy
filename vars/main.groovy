#!/usr/bin/env groovy

def getServer() {
    def remote = [:]
    remote.serverName = "${SERVER_NAME}"
    remote.serverPwd = "${SERVER_PWD}"
    remote.host = "${REMOTE_HOST}"
    remote.port = 22
    remote.allowAnyHosts = true
    return remote
}



def call(Map map) {

    pipeline {
        agent {
            node {
                label 'master'
                // 指定jenkins工作空间目录
                customWorkspace '${map.workspace}'
            }
        }
        // 保留最近5次构建
        options { 
            buildDiscarder(logRotator(numToKeepStr: '5')) 
            timeout(time:1, unit:'HOURS')
            disableConcurrentBulids()
        }
        // 声明参数 
        parameters{
            // 项目名称
            string(name: 'projectName', defaultValue: '"'${map.PROJECT_NAME}'"', description:'项目名称')
            // // SVN代码路径
            // string(name: 'SVN_repoUrl', defaultValue: '"'${map.SVN_URL}'"', description: 'SVN代码路径')
            // // SVN代码路径
            // string(name: 'GIT_repoUrl', defaultValue: '"'${map.SVN_URL}'"', description: 'SVN代码路径')
            // 部署内容的相对路径
            string(name: 'deployLocation', defaultValue: 'target/*.jar,target/alternateLocation/*.*,'+'target/classes/*.*,target/classes/i18n/*.*,target/classes/rawSQL/*.*,'+'target/classes/rawSQL/mapper/*.*,target/classes/rawSQL/mysql/*.*,'+'target/classes/rawSQL/sqlserver/*.*', description: '部署内容的相对路径 ')
        }
        // 设置全局变量
        environment {
            //服务器信息
            SERVER      = "${map.SERVER}"
            // 代码分支
            BRANCH_NAME = "${map.BRANCH_NAME}"
            // SVN代码路径
            SVN_URL     = "${map.SVN_URL}"
            // GIT代码路径
            GIT_URL     = "${map.GIT_URL}"
            // 服务器部署目录
            DEPLOY_URL  = "${map.DEPLOY_URL}"
            // Jenkins全局凭据
            CRED_ID     = "${map.CRED_ID}"
            // 项目经理邮箱
            PM_EMAIL='PM'
            // Jenkins运维邮箱
            JM_EMAIL='QA'
            // 测试人员邮箱
            TEST_EMAIL='Tester'
        }
        // 声明使用的工具 
        tools {
            maven 'maven3.6.2'
            jdk   'jdk1.8'
        }
    
        // pipeline运行结果通知给触发者
        post{
            //执行后清理workspace
            always{
                echo "清理工作空间workspace...................................."
                deleteDir()
            }
            failure{
                script { 
                    emailext body: '${JELLY_SCRIPT,template="static-analysis"}', 
                    recipientProviders: [
                        [$class: 'RequesterRecipientProvider'],
                        [$class: 'DevelopersRecipientProvider']
                    ], 
                    subject: '${JOB_NAME}- Build # ${BUILD_NUMBER} - Failure!'
                }
            }
        }
        // 流水线构建部署项目
        stages {
            stage('工具版本信息') {
                steps {
                    echo '查看工具版本信息--------------------------------------'
                    sh 'java --version'
                    sh 'mvn --version'
                }
            }

            stage('清理本地mavne仓库') {
                steps{
                    // Jenkins在构建maven工程时会去下载该工程所依赖的各种jar包，
                    // 同时也会从服务器去更新这些jar包，但由于网络环境的问题，
                    // 有些jar包更新会失败，会留下后缀名为“.lastUpdated”残留文件，
                    // 当job构建的时候就会卡在这里，所以写个删除此类文件的shell脚本，
                    // 每次构建maven工程的时候，先调用这个脚本删除残留文件。
                    echo '清理本地maven仓库中更新失败的文件---------------------'
                    sh '/home/jenkins/del_lastUpdated.sh'
                    echo '清理完成--------------------------------------------'
                }
            }

            stage('拉取代码') {
                steps {
                    steps {
                        script {
                            if(${SVN_URL}!=null){
                                //从SVN拉取代码
                                echo '从svn拉取代码开始----------------------------------'
                                def scmVars = checkout ([
                                    $class: 'SubversionSCM', 
                                    additionalCredentials: [],
                                    excludedCommitMessages: '', 
                                    excludedRegions: '', 
                                    excludedRevprop: '', 
                                    excludedUsers: '', 
                                    filterChangelog: false, 
                                    ignoreDirPropChanges: false, 
                                    includedRegions: '', 
                                    locations: [[
                                        credentialsId: "${CRED_ID}", 
                                        depthOption: 'infinity', 
                                        ignoreExternalsOption: true, 
                                        local: '.', 
                                        remote: "${SVN_URL}"
                                    ]], 
                                    workspaceUpdater: [$class: 'UpdateUpdater']
                                ])
                                svnversion = scmVars.SVN_REVISION
                                echo "输出版本号:${svnversion}----------------------------"
                                echo '从svn拉取代码结束------------------------------------'
                            } else {
                                echo '从git拉取代码开始----------------------------------'
                                def scmVars = checkout([
                                   $class: 'GitSCM', 
                                   branches: [[name: '*/master']], 
                                   doGenerateSubmoduleConfigurations: false, 
                                   extensions: [$class: 'CleanBeforeCheckout'], 
                                   submoduleCfg: [], 
                                   userRemoteConfigs: [[
                                       credentialsId: "${CRED_ID}", 
                                       url: "${GIT_URL}"
                                    ]]
                                ])
                                gitversion = scmVars.GIT_REVISION
                                echo "输出版本号:${gitversion}----------------------------"
                                echo '从git拉取代码结束----------------------------------'
                            }
                        }
                    }
                }
            }

            stage('编译代码') {
                steps {
                    echo 'maven编译代码开始---------------------------------'
                    sh "mvn -Dmaven.test.failure.ignore clean package"
                    echo 'maven编译代码结束---------------------------------'
                }
            }
            
            stage('归档项目文件') {
                steps {
                    // 归档文件
                    echo '归档项目文件开始--------------------------------------'
                    /*archiveArtifacts artifacts: 'target/*.jar,target/alternateLocation/*.*,'+'target/classes/*.*,target/classes/i18n/*.*,target/classes/rawSQL/*.*,'+'target/classes/rawSQL/mapper/*.*,target/classes/rawSQL/mysql/*.*,'+'target/classes/rawSQL/sqlserver/*.*',fingerprint: true*/
                    archiveArtifacts params.deployLocation
                    echo '归档项目文件结束--------------------------------------'
                }
            }
            
            stage('对当前版本代码打tag') {
                steps{
                    timeout(5) {
                        script {
                            input message:'需要打tag嘛？'
                        }
                    }
                    if(${SVN_URL}!=null){
                        echo '项目打版本标签开始-------------------------------------------------'
                        // 传入shell脚本项目名,代码路径及版本
                        sh "/home/jenkins/del_creat_tag.sh ${params.projectName} ${SVN_URL} ${svnversion}"
                        echo '项目打版本标签结束-------------------------------------------------'
                    } else {
                        echo '项目打版本标签开始-------------------------------------------------'
                        // 传入shell脚本项目名,代码路径及版本
                        sh "/home/jenkins/del_creat_tag.sh ${params.projectName} ${GIT_URL} ${gitversion}"
                        echo '项目打版本标签结束-------------------------------------------------'
                    }
                    
                }
            }
 
            stage('确认是否部署项目') {
                steps{
                    timeout(5) {
                        script {
                            def json_file = "${map.workspace}/service_json.json"
                            def service_json = readJSON json_file
                            println service_json.size()
                            println service_json.lenth()
                            input message:'部署项目？'
                            for(int i= 0; service_json.size(); ++i){
                                def server_split= service_json[i].split(",")
                                server_IP = server_split[0]
                                server_Port = server_split[1]
                                server_Name = server_split[2]
                                server_Passwd = server_split[3]
                                echo '重启服务开始--------------------------'
                                // 重启服务
                                sh "/home/jenkins/restart_server.sh ${server_IP} ${server_Name} ${DEPLOY_URL}"
                                echo '服务已经启动,请前往浏览器查看..................................................'
                            }
                            mail to: "${JM_EMAIL},${PM_EMAIL}",
                            subject: "PineLine '${JOB_NAME}' (${BUILD_NUMBER})人工验收通知",
                            body: "提交的PineLine '${JOB_NAME}' (${BUILD_NUMBER})进入人工验收环节\n请及时前往${env.BUILD_URL}进行测试验收"
                        }
                    }
                }
            }

            stage('init-server') {
                steps {
                    script {
                        server = getServer()
                    }
                }
            }
        }
    }
}