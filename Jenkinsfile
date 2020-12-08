pipeline {
    agent any
//            {
//        node {
//            label any
//            customWorkspace "workspace/${JOB_NAME}/${BUILD_NUMBER}"
//        }
//    }
    environment {
        KISTERS_DOCKER_HOME = "/opt/kisters/docker"
        BUILD_URL = "https://jenkins.energy-dev.kisters.de/job/${JOB_NAME}/${BUILD_NUMBER}/console"
        EMAIL_TO = "Polina.Mrachkovskaya@kisters.de"

    }

    parameters {
        string(name: 'MS')
        string(name: 'VERSION')
    }


//        stage('SSH Connection') {
//            environment {
//                CD_SECRET_KEY = credentials('jenkins-cd-key')//better withCredentials
//            }
//
//            steps {
//                sh 'pwd'
//                sh 'ls -la'
//                sh "ssh -i ${env.CD_SECRET_KEY} -v -T -o StrictHostKeyChecking=no root@en-cdeval-prod hostname"
//            }
//
//        }
    stages {
        stage('Deploy Staging') {
            steps {
                sh "echo ${params.MS}"
                withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
                    sh 'rm -f .env'
                    sh "printf \"VERSION=${params.VERSION}\" >> .env"
                    stash includes: '.env', name: 'env'
                    stash includes: 'docker-compose.yml', name: 'compose'
                    sh "ssh -i $test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'rm -rf ${env.KISTERS_DOCKER_HOME}/test && mkdir ${env.KISTERS_DOCKER_HOME}/test'"
                    sh "scp -i $test -o StrictHostKeyChecking=no .env root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/test"
                    sh "scp -i $test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/test"
                    sh "ssh -i $test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'cd ${env.KISTERS_DOCKER_HOME}/test && docker-compose up -d ${params.MS}'"

                }
            }

        }


        stage('Test Staging') {
            steps {
                script {
                    retry(3) {    // if fails then retries again

                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
                            sleep(4)
                            def statusCode = sh(script: "curl -sL -w '%{http_code}' 'http://en-cdeval-test:8081/test?country=Aus' -o /dev/null", returnStdout: true)
                            echo statusCode
                            if (statusCode != "200") {
                                error "Curl command was not successful, it delivered status code ${statusCode}"
                            }
                        }
                    }
//                    http://localhost:8081/rest/data?country=Aus&sector=private&year=2018
                }
            }
        }
        stage('Send Email') {
            steps {
                emailext subject: "[Jenkins]${currentBuild.fullDisplayName}", to: "${env.EMAIL_TO}", from: "jenkins@mail.com",
                        body: "<a href='${env.BUILD_URL}'>Click to approve</a>"
            }
        }
        stage('Parallel Deployment') {
            failFast true
            parallel {

                stage('Deploy Production') {
                    steps {
                        script {
                            def input = input message: 'User input required',
                                    parameters: [choice(name: 'Proceed deployment to PROD? ', choices: ['NO', 'YES'], description: 'Choose "yes" if you want to deploy this build in production')]
                            echo input
                            if (input == 'NO') {
                                error "The build was stopped by ${username}"
                            }
                        }
                        unstash 'env'
                        unstash 'compose'
                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {

                            sh "ssh -i $test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'rm -rf ${env.KISTERS_DOCKER_HOME}/test && mkdir -p ${env.KISTERS_DOCKER_HOME}/test'"
                            sh "scp -i $test .env root@en-cdeval-prod:${env.KISTERS_DOCKER_HOME}/test"
                            sh "scp -i $test docker-compose.yml root@en-cdeval-prod:${env.KISTERS_DOCKER_HOME}/test"
                            sh "ssh -i $test -T root@en-cdeval-prod 'cd ${env.KISTERS_DOCKER_HOME}/test && docker-compose up -d ${params.MS}'"
                        }
                    }

                }

                stage('Deploy Demo') {
                    steps {
                        script {
                            sleep(5)
                            echo 'DEMO deployment as a parallel stage'
                        }
                    }
                }
            }
        }
        stage('Test against Prod-System') {
            steps {
                echo "FINISH"
            }
        }
    }
}
//        stage('Test against Prod-System') {
//            steps {
//                script {
//                    retry(3) {// if fails then retries again
//
//                        withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
//                            sleep(4)
//                            def statusCode = sh(script: "curl -sL -w '%{http_code}' 'http://en-cdeval-prod:8081/test?country=Aus' -o /dev/null", returnStdout: true)
//                            echo statusCode
////                        println statusCode.getClass()
//                            if (statusCode != "200") {
//                                error "Curl command was not successful, it delivered status code ${statusCode}"
//                            }
//                        }
//                    }
////                    http://localhost:8081/rest/data?country=Aus&sector=private&year=2018
//                }
//            }
//        }
//
//    }
//    post {
//        failure {
//            script {
//
//                emailext subject: "[Jenkins]${currentBuild.fullDisplayName}", to: "${env.EMAIL_TO}", from: "jenkins@mail.com",
//                        body: "<a href='${env.BUILD_URL}'>click to trace the failure</a>";
//            }
//        }
//    }


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
