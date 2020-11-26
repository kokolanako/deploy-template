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
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(deploy)
                    }
                    branch("master")
                }
            }
            scriptPath("Jenkinsfile")
        }
    }
}