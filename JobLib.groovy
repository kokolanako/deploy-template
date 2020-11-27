def be='https://github.com/kokolanako/be.git'
def deploy='https://github.com/kokolanako/deploy-template.git'
def l1='build-slave-maven'
def d1='en-compile-stage-docker'
def ms1='ms1'

job("MS1-MVN-BUILD") {
    label l1
    jdk("jdk11")

//    customWorkspace ("${JENKINS_HOME}/workspace/${JOB_NAME}/${BUILD_NUMBER}")
    scm {
        git {remote {url(be)}
            branch('ms1')
        }
    }

    steps{
        maven{
            mavenInstallation('maven-3.6.3')
            goals('clean package') //Java 11

        }
        shell('ls -la')
        shell('pwd')

    }
    publishers{
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
def registry= '705249/lol'
def image= "705249/lol:${BUILD_NUMBER}"
def registryCredential = 'dockerhub'

job('ms1-docker-commit-test'){
    label d1
    steps{
        copyArtifacts("MS1-MVN-BUILD"){

        }
        shell('echo $name')
        shell("echo $image")
        shell('docker build . -t '+image)
        shell('docker rmi '+image)
    }
    publishers{

        downstreamParameterized {
            trigger('ms1-docker-deploy-test') {
                parameters {
                    predefinedProp('label-d', "$d1")

                }
            }
        }
    }
}
job('ms1-docker-deploy-test'){
    label '$label-d'
    
        wrappers{
        credentialsBinding{
            usernamePassword('user','pw','dockerhub')
        }
    }
    steps{
        shell("docker login --user='$user' --password='$pw'")
        shell('docker push '+image)
        shell('docker logout')
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