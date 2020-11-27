def be='https://github.com/kokolanako/be.git'
def deploy='https://github.com/kokolanako/deploy-template.git'
def l1='build-slave-maven'
def l2='en-jenkins-l-2'

job("MS1-MVN-BUILD") {
    label l1
    jdk("jdk11")

    customWorkspace ("${JENKINS_HOME}/workspace/${JOB_NAME}/${BUILD_NUMBER}")
    scm {
        git {remote {url(be)}branch('ms1')
        }
    }

    steps{
        maven{
            mavenInstallation('maven-3.6.3')
            goals('clean package') //Java 11

        }
        shell('ls -la')

    }
//    publishers{
//        archiveArtifacts {
//            pattern('**/*-SNAPSHOT.jar')
//            onlyIfSuccessful()
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