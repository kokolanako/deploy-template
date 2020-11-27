def be='https://github.com/kokolanako/be.git'
def deploy='https://github.com/kokolanako/deploy-template.git'
def agent_1_mvn='build-slave-maven'
def agent_2='build-slave-chef'

job("MS1-checkout") {
    label agent_2
    scm {
        git {
            remote {
                url(be)
            }
            branch('ms1')
        }

    }
    steps {
        shell('cd /home && ls -lah')
        
        shell('cd /opt && ls -lah')
        shell('ls -la')
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