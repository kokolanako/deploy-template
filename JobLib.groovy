def be = 'https://github.com/kokolanako/be.git'
def deploy = 'https://github.com/kokolanako/deploy-template.git'
def l1 = 'build-slave-maven'
def d1 = 'en-compile-stage-docker'
def ms1 = 'ms1'

job("MS1-MVN-BUILD") {
    label l1
    jdk("jdk11")

//    customWorkspace ("${JENKINS_HOME}/workspace/${JOB_NAME}/${BUILD_NUMBER}")
    scm {
        git {
            remote { url(be) }
            branch('ms1')
        }
    }

    steps {
        maven {
            mavenInstallation('maven-3.6.3')
            goals('clean package') //Java 11
        }
        shell('ls -la')
        shell('pwd')

    }
    publishers {
        archiveArtifacts {
            pattern('**/target/**SNAPSHOT.jar')
            pattern('Dockerfile')
            onlyIfSuccessful()
        }
        downstreamParameterized {
            trigger('ms1-docker-commit-test') {
                parameters {
                    predefinedProp('name', 'ms1')
                }
            }
        }
    }
}
def registry = '705249/lol'
def image = "705249/lol:${BUILD_NUMBER}"//always seed job number
def registryCredential = 'dockerhub'

job('ms1-docker-commit-test') {
    label d1
    steps {
        copyArtifacts("MS1-MVN-BUILD") {
        }
        shell('echo $name')
        shell("echo $image")
        shell('docker build . -t ' + image)
    }
    publishers {
        downstream ('ms1-docker-deploy-test','UNSTABLE')

    }
}
job('ms1-docker-deploy-test') {
    label d1

    wrappers {
        credentialsBinding {
            usernamePassword('DOCKER_USER', 'DOCKER_PW','dockerhub' )

        }
    }
    steps {
        shell('echo $DOCKER_USER')
        shell('docker login -u $DOCKER_USER -p $DOCKER_PW')
        shell('docker push '+image)
        shell('docker logout')
        shell('docker rmi ' + image)
    }
    publishers {
        downstreamParameterized {
            trigger('ssh-connection') {
                parameters {
                    predefinedProp('image_name', image)
                    predefinedProp('container', ms1)
                }
            }
        }
    }

}
job('ssh-connection') {
    label d1 //only on this node

    steps {
        remoteShell('root@en-cdeval-test:22') {
            command('hostname')
        }
    }
    publishers {
        downstreamParameterized {
            trigger('test-deploy') {
                parameters {
                    predefinedProp('image_name', '$image_name')
                    predefinedProp('container','$container')
                }
            }
        }
    }
}
//withCredentials(bindings: [sshUserPrivateKey(credentialsId: 'jenkins-cd-key', keyFileVariable: 'test')]) {
//    sh 'rm -f .env'
//    sh "printf \"IMAGE_NAME=${params.IMAGE}\n CONTAINER_NAME=${params.CONTAINER}\" >> .env"
//    stash includes: '.env', name: 'env'
//    stash includes: 'docker-compose.yml', name: 'compose'
//    sh "ssh -i $test -T root@en-cdeval-test 'rm -rf ${env.KISTERS_DOCKER_HOME}/yay && mkdir ${env.KISTERS_DOCKER_HOME}/yay'"
//    sh "scp -i $test .env root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/yay"
//    sh "scp -i $test docker-compose.yml root@en-cdeval-test:${env.KISTERS_DOCKER_HOME}/yay"
//    sh "ssh -i $test -T root@en-cdeval-test 'cd ${env.KISTERS_DOCKER_HOME}/yay && docker-compose down && docker-compose up -d'"
//}

def KISTERS_DOCKER_HOME = "/opt/kisters/docker"
def BUILD_URL = "https://jenkins.energy-dev.kisters.de/job/${JOB_NAME}/${BUILD_NUMBER}/console"
def EMAIL_TO = "Polina.Mrachkovskaya@kisters.de"


job('test-deploy') {
    label d1 //only on this node

    steps {
        shell('echo $container')
        remoteShell('root@en-cdeval-test:22') {
            command('rm -rf $KISTERS_DOCKER_HOME/yay && mkdir $KISTERS_DOCKER_HOME/yay')
            command('cd $KISTERS_DOCKER_HOME/yay && printf \\"IMAGE_NAME=$image_name\\n CONTAINER_NAME=$container\\" >> .env')

        }
        shell('scp docker-compose.yml root@en-cdeval-test:KISTERS_DOCKER_HOME/yay')
    }
//    publishers {
//        downstreamParameterized {
//            trigger('test-deploy') {
//                parameters {
//                    predefinedProp('image_name', '$image')
//                }
//            }
//        }
//    }
}



//pipelineJob("PipelineJob-test"){
//    environmentVariables {
//        // these vars could be specified by parameters of this job
//        env('job', 'ms1')
//        env('image', '705249/lol:48') //comma separated string
//        env('container', 'ms1')
//    }
//    definition {
////        cpsScm {
////            scm {
////                git {
////                    remote {
////                        url(deploy)
////                    }
////                    branch("*/jobDSL")
////                }
////            }
////            scriptPath("Jenkinsfile")
////        }
//        cps{
//            script(readFileFromWorkspace("Jenkinsfile"))
//        }
//    }
//}