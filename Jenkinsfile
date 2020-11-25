pipeline {
    agent {
        node{
            label 'en-jenkins-l-1'
            customWorkspace "workspace/${JOB_NAME}/${BUILD_NUMBER}"
        }
    }
    environment{
        KISTERS_DOCKER_HOME="/opt/kisters/docker"

    }

    parameters {
        // choice(name:'MS', choices:['ms1','ms2'],description:'Pick the microservice to deploy')
        string(name: 'MS', defaultValue: 'ms1')
        string(name: 'IMAGE', defaultValue: '705249/lol:48')
        string(name: 'CONTAINER', defaultValue: 'ms1')

    }
    stages {
//        stage('Start the app') {
//            steps {
//                sh 'docker ps -a'
//                echo "Deploying ${params.MS}"
//                sh 'rm -f .env'
//                sh "printf \"IMAGE_NAME=${params.IMAGE}\n CONTAINER_NAME=${params.CONTAINER}\">> .env"
//
////                sh 'docker-compose pull'
//                sh 'docker-compose down'
//                sh 'docker-compose up -d'
//
//            }
//        }
//        stage('Test') {
//            steps {
//                sleep(3)
//                script {
//
////                    http://localhost:8081/rest/data?country=Aus&sector=private&year=2018
//
//                    def statusCode = sh(script: "curl -sLI -w '%{http_code}' 'http://en-jenkins-l-1:8081/test?country=Aus' -o /dev/null", returnStdout: true)
//                    echo statusCode
//                    println statusCode.getClass()
//                    if (statusCode != "200") {
//                        catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
//                            sh "exit 1"
//                        }
//                    }
//
//                }
//            }
//        }

        stage('SSH') {
            environment {
                CD_SECRET_KEY = credentials('jenkins-cd-key')
            }

            steps {
                sh 'pwd'
                sh 'ls -la'
                sh "ssh -i ${env.CD_SECRET_KEY} -v -T -o StrictHostKeyChecking=no root@en-cdeval-prod hostname"
            }

        }

        stage('Deploy  the app on TEST-remote') {
            steps {
                sh 'ls -la'
                withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
                    sh 'rm -f .env'
                    sh "printf \"IMAGE_NAME=${params.IMAGE}\n CONTAINER_NAME=${params.CONTAINER}\" >> .env"
                    stash includes: '.env', name: 'env'
                    stash includes: 'docker-compose.yml', name: 'compose'
                    sh "ssh -i $test -T root@en-cdeval-test 'rm -rf ${env.KISTERS_DOCKER_HOME}/yay && mkdir ${env.KISTERS_DOCKER_HOME}/yay'"
                    sh "scp -i $test .env root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/yay"
                    sh "scp -i $test docker-compose.yml root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/yay"
                    sh "ssh -i $test -T root@en-cdeval-test 'cd ${env.KISTERS_DOCKER_HOME}/yay && docker-compose down && docker-compose up -d'"
                }
            }

        }


        stage('Test on TEST-SERVER') {
            steps {
                script {
                    retry(3) {// if fails then retries again

                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
                            sleep(4)
                            def statusCode = sh(script: "curl -sL -w '%{http_code}' 'http://en-cdeval-test:8081/test?country=Aus' -o /dev/null", returnStdout: true)
                            echo statusCode
//                        println statusCode.getClass()
                            if (statusCode != "200") {
                                error "Curl command was not successful, it delivered status code ${statusCode}"
                            }
                        }
                    }
//                    http://localhost:8081/rest/data?country=Aus&sector=private&year=2018
                }
            }
        }
        stage('Deploy on PRODUCTION-SERVER'){
            agent {
                node{
                    label 'en-jenkins-l-2'
                    customWorkspace "workspace/${JOB_NAME}/${BUILD_NUMBER}"
                }
            }
            input{

                message"Proceed?"
                parameters{
                    string(name: 'username', defaultValue:'user', description:' Username of the use pressing OK')
                    choice(choices: ['Proceed','Stop'], name:'proceed')
                }
            }

            steps{
                script{
                    if("${proceed}" =='Stop'){
                        error "The build was stopped by ${username}"
                    }

                }

                echo "User: ${username} triggered the deployment stage"
                sh 'pwd'
                sh 'ls -la' //woher hat -l-2 .env?
                unstash 'env'
                unstash 'compose'
                withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {

                    sh "ssh -i $test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'rm -rf ${env.KISTERS_DOCKER_HOME}/yay && mkdir -p ${env.KISTERS_DOCKER_HOME}/yay'"
                    sh "scp -i $test .env root@en-cdeval-prod:${env.KISTERS_DOCKER_HOME}/yay"
                    sh "scp -i $test docker-compose.yml root@en-cdeval-prod:${env.KISTERS_DOCKER_HOME}/yay"
                    sh "ssh -i $test -T root@en-cdeval-prod 'cd ${env.KISTERS_DOCKER_HOME}/yay && docker-compose down && docker-compose up -d'"
                }
            }

        }

        stage('Test on PROD-SERVER') {
            steps {
                script {
                    retry(3) {// if fails then retries again

                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
                            sleep(4)
                            def statusCode = sh(script: "curl -sL -w '%{http_code}' 'http://en-cdeval-prod:8081/test?country=Aus' -o /dev/null", returnStdout: true)
                            echo statusCode
//                        println statusCode.getClass()
                            if (statusCode != "200") {
                                error "Curl command was not successful, it delivered status code ${statusCode}"
                            }
                        }
                    }
//                    http://localhost:8081/rest/data?country=Aus&sector=private&year=2018
                }
            }
        }

    }
    post{
        failure {
            mail  charset: 'UTF-8', mimeType: 'text/html', subject: "ERROR CI: Project name -> ${env.JOB_NAME}", to: "Polina.Mrachkovskaya@kisters.de", recipientProviders: [developers()];
        }
    }
//    post{
//        always{
//            node('en-jenkins-l-2'){
//                script{
////                    cleanWs()
//                    deleteDir()
//                }
//            }
//            node('en-jenkins-l-1'){
//                script{
//                    deleteDir()
//                }
//            }
//        }
//    }
}