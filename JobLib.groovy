def be='https://github.com/kokolanako/be.git'
def deploy='https://github.com/kokolanako/deploy-template.git'

//job("MS1-checkout") {
//    label 'build-slave-maven'
//    scm {
//        git {
//            remote {
//                url(be)
//            }
//            branch('ms1')
//        }
//
//    }
//    steps {
//        shell('ls -la')
//    }
//}
pipelineJob("PipelineJob-test"){
    environmentVariables {
        // these vars could be specified by parameters of this job
        env('job', 'ms1')
        env('image', '705249/lol:48') //comma separated string
        env('container', 'ms1')
    }
    definition {
//        cpsScm {
//            scm {
//                git {
//                    remote {
//                        url(deploy)
//                    }
//                    branch("*/jobDSL")
//                }
//            }
//            scriptPath("Jenkinsfile")
//        }
        cps{
            script(readFileFromWorkspace("JobLib.groovy"))
        }
    }
}