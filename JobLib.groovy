def be = 'https://github.com/kokolanako/be.git'
def deploy = 'https://github.com/kokolanako/deploy-template.git'
def l1 = 'build-slave-maven'
def d1 = 'en-compile-stage-docker'
def ms1 = 'ms1'
def ms2 = 'ms2'
def msArr=['ms1','ms2']
def ms1_dockerhub = '705249/lol'
def ms2_dockerhub = '705249/be'
//def image = "705249/lol:${BUILD_NUMBER}"//always seed job number
//def registryCredential = 'dockerhub'


job("ms1-commit") {
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
            trigger('ms1-docker-commit') {
                parameters {
                    predefinedProp('dockerhub_registry', ms1_dockerhub)
                }
            }
        }
    }
}
job("ms2-commit") {
    label l1
    jdk("jdk11")

//    customWorkspace ("${JENKINS_HOME}/workspace/${JOB_NAME}/${BUILD_NUMBER}")
    scm {
        git {
            remote { url(be) }
            branch('ms2')
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
            trigger('ms1-docker-commit') {
                parameters {
                    predefinedProp('dockerhub_registry', ms2_dockerhub)
                }
            }
        }
    }
}

for (String ms: msArr){
    def image="\${dockerhub_registry}:"+"${BUILD_NUMBER}"
    job(ms-'-docker-commit') {
        label d1
        steps {
            copyArtifacts(ms+"-commit") {
            }
            shell("echo $image")
            shell('docker build . -t '+image )
        }
        publishers {
            def next=ms+"-docker-deploy"
            downstream(next, 'SUCCESS')

        }
    }
    job(ms+'-docker-deploy') {
        label d1

        wrappers {
            credentialsBinding {
                usernamePassword('DOCKER_USER', 'DOCKER_PW', 'dockerhub')

            }
        }
        steps {
            shell('echo $DOCKER_USER')
            shell('docker login -u $DOCKER_USER -p $DOCKER_PW')
            shell('docker push ' + image)
            shell('docker logout')
            shell('docker rmi ' + image)
        }
        publishers {
            downstreamParameterized {
                trigger('ssh-connection-check') {
                    parameters {
                        predefinedProp('VERSION', "${BUILD_NUMBER}")
                        predefinedProp('MS', ms)
                    }
                }
            }
        }

    }
}

//job('ms1-docker-commit') {
//    label d1
//    steps {
//        copyArtifacts("ms1-commit") {
//        }
//
//        shell('docker build . -t ' + image)
//    }
//    publishers {
//        downstream('ms1-docker-deploy', 'SUCCESS')
//
//    }
//}
//job('ms1-docker-deploy') {
//    label d1
//
//    wrappers {
//        credentialsBinding {
//            usernamePassword('DOCKER_USER', 'DOCKER_PW', 'dockerhub')
//
//        }
//    }
//    steps {
//        shell('echo $DOCKER_USER')
//        shell('docker login -u $DOCKER_USER -p $DOCKER_PW')
//        shell('docker push ' + image)
//        shell('docker logout')
//        shell('docker rmi ' + image)
//    }
//    publishers {
//        downstreamParameterized {
//            trigger('ssh-connection-check') {
//                parameters {
//                    predefinedProp('VERSION', "${BUILD_NUMBER}")
//                    predefinedProp('MS', ms1)
//                }
//            }
//        }
//    }
//
//}
job('ssh-connection-check') {
    label d1 //only on this node


    steps {
        remoteShell('root@en-cdeval-test:22') {//SSH Plugin
            command('hostname')
        }
    }
    publishers {
        downstreamParameterized {
            trigger('test-deploy') {
                parameters {
                    predefinedProp('VERSION', "${BUILD_NUMBER}")
                    predefinedProp('MS', '${MS}')
                }
            }
        }
    }
}

def KISTERS_DOCKER_HOME = "/opt/kisters/docker"
def BUILD_URL = "https://jenkins.energy-dev.kisters.de/job/${JOB_NAME}/${BUILD_NUMBER}/console"
def EMAIL_TO = "Polina.Mrachkovskaya@kisters.de"


job('test-deploy') {
    label d1 //only on this node
    scm {
        git {
            remote { url(deploy) }
            branch('jobDSL')
        }
    }
    wrappers {
        credentialsBinding {
            file('test', 'remote-deploy')
            usernamePassword('DOCKER_USER', 'DOCKER_PW', 'dockerhub')
        }
    }

    steps {
        shell('docker login -u $DOCKER_USER -p $DOCKER_PW')
        shell("""
rm -f .env
printf \"VERSION=${BUILD_NUMBER}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'rm -rf $KISTERS_DOCKER_HOME/yay && mkdir $KISTERS_DOCKER_HOME/yay'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-test:$KISTERS_DOCKER_HOME/yay
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-test:$KISTERS_DOCKER_HOME/yay
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'cd $KISTERS_DOCKER_HOME/yay && docker-compose up -d \${MS}'
""")

    }
    publishers {
        downstreamParameterized {
            trigger('test-curl') {
                parameters {
                    predefinedProp('VERSION', '${VERSION}')
                    predefinedProp('MS', '${MS}')
                }
            }
        }

    }

}


job('test-curl') {
    label d1 //only on this node ??

    wrappers {
        credentialsBinding {
            file('test', 'remote-deploy')
        }
    }
    steps {
        shell("""
sleep 4
statusCode=\$(curl -sL -w '%{http_code}' 'http://en-cdeval-test:8081/test?country=Aus' -o /dev/null)
if [ "\$statusCode" -ne "200" ]; then 
    exit 1 
fi
""")
    }
    publishers {
        mailer(EMAIL_TO, true, true)
        buildPipelineTrigger('prod-deploy'){
            parameters {
                predefinedProp('VERSION', '${VERSION}')
                predefinedProp('MS', '${MS}')
            }
        }

        downstreamParameterized {
            trigger('demo') {
                parameters {
                    predefinedProp('VERSION', '${VERSION}')
                    predefinedProp('MS', '${MS}')
                }
            }
        }

    }
}
job('demo') {
    label l1
    steps {
        shell('sleep 5 && echo DEMO && echo building ${MS} with version ${VERSION}')
    }
    publishers {
        downstream('finish', "SUCCESS")
    }
}

job('prod-deploy') {
    label d1 //only on this node

    scm {
        git {
            remote { url(deploy) }
            branch('jobDSL')
        }
    }
    parameters {
        choiceParam('OPTION', ['deploy to prod', 'stop'])
    }
    wrappers {
        credentialsBinding {
            file('test', 'remote-deploy')
            usernamePassword('DOCKER_USER', 'DOCKER_PW', 'dockerhub')
        }
    }

    steps {
        shell('echo $OPTION')

        shell("""
if  [ "\$OPTION" = "stop" ]; then
    echo "The pipeline was stopped intentionally. The user did not want to deploy to production."
    exit 0
fi
echo \$OPTION
rm -f .env
printf \"VERSION=${BUILD_NUMBER}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'rm -rf $KISTERS_DOCKER_HOME/yay && mkdir $KISTERS_DOCKER_HOME/yay'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-prod:$KISTERS_DOCKER_HOME/yay
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-prod:$KISTERS_DOCKER_HOME/yay
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'cd $KISTERS_DOCKER_HOME/yay && docker-compose up -d \${MS}'
""")

    }
    publishers {
        downstream('finish', "SUCCESS")
    }


}
job('finish'){
    blockOnUpstreamProjects()
//    blockOn(['demo','prod-deploy'])
    steps{
        shell('echo FINISH')
    }
}

nestedView('Seminar-Pipelines') {
    views {
        buildPipelineView('ms1-commit pipeline') {
            displayedBuilds(5)
            selectedJob('ms1-commit')
            alwaysAllowManualTrigger()
            showPipelineParameters()
        }
        buildPipelineView('ms2-commit pipeline') {
            displayedBuilds(5)
            selectedJob('ms2-commit')
            alwaysAllowManualTrigger()
            showPipelineParameters()
        }
//        buildPipelineView('ms-deployment') {
//            displayedBuilds(5)
//            selectedJob('ssh-connection-check')
//            alwaysAllowManualTrigger()
//            showPipelineParameters()
//        }

    }
}

