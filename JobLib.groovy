def be='https://github.com/kokolanako/be.git'
def deploy='https://github.com/kokolanako/deploy-template.git'
def l1='en-jenkins-l-1'
def l2='en-jenkins-l-2'

job("MS1-MVN-BUILD") {
    label l1
    jdk('Java 1.11')
    customWorkspace ("workspace/${JOB_NAME}/${BUILD_NUMBER}")
    scm {
        git {
            remote {
                url(be)
            }
            branch('ms1')
        }

    }
//    wrappers{
//        buildInDocker{
//            image('maven:3.6.3-jdk-11')
//            volume('/dev/urandom', '/dev/random')
//            verbose()
//        }
//    }
    steps{
        maven{
            goals('clean package')

        }

    }
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