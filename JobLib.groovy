def be = 'https://github.com/kokolanako/be.git'
def deploy = 'https://github.com/kokolanako/deploy-template.git'
def l1 = 'build-slave-maven'
def d1 = 'en-compile-stage-docker'
def ms1 = 'ms1'
def ms2 = 'ms2'
def msArr=['ms1','ms2']
def ms1_dockerhub = '705249/lol'
def ms2_dockerhub = '705249/be'



job("ms1-build-app") {
    label l1
    jdk("jdk11")
    scm {
        git {
            remote { url(be) }
            branch('ms1')
        }
    }
    steps {
        maven {
            mavenInstallation('maven-3.6.3')
            goals('clean package')
        }
    }
    publishers {
        archiveArtifacts {
            pattern('**/target/**SNAPSHOT.jar')
            pattern('Dockerfile')
            onlyIfSuccessful()
        }
        downstreamParameterized {
            trigger('ms1-build-image') {
                parameters {
                    predefinedProp('dockerhub_registry', ms1_dockerhub)
                    predefinedProp('VERSION', '${BUILD_NUMBER}')
                    predefinedProp('CUSTOM_PORT', "8081")

                }
            }
        }
    }
}
job("ms2-build-app") {
    label l1
    jdk("jdk11")
    scm {
        git {
            remote { url(be) }
            branch('ms2')
        }
    }
    steps {
        maven {
            mavenInstallation('maven-3.6.3')
            goals('clean package')
        }
    }
    publishers {
        archiveArtifacts {
            pattern('**/target/**SNAPSHOT.jar')
            pattern('Dockerfile')
            onlyIfSuccessful()
        }
        downstreamParameterized {
            trigger('ms2-build-image') {
                parameters {
                    predefinedProp('dockerhub_registry', ms2_dockerhub)
                    predefinedProp('VERSION', '${BUILD_NUMBER}')
                    predefinedProp('CUSTOM_PORT', "8082")
                }
            }
        }
    }
}

for (String ms: msArr){
    job(ms+"-build-image") {
        label d1 //agent with installed docker
        wrappers {
            credentialsBinding {
                usernamePassword('DOCKER_USER', 'DOCKER_PW', 'dockerhub')
            }
        }
        steps {
            shell("echo \${CUSTOM_PORT}")
            def image="\${dockerhub_registry}:\${VERSION}"
            copyArtifacts(ms+"-build-app") {
            }
            shell("docker build . -t "+image )
            shell('docker login -u $DOCKER_USER -p $DOCKER_PW')
            shell('docker push ' + image)
            shell('docker logout')
            shell('docker rmi ' + image)
        }
        publishers {
            def next="deploy-staging"
            downstreamParameterized {
                trigger(next) {
                    parameters {
                        predefinedProp('dockerhub_registry','$dockerhub_registry')
                        predefinedProp('VERSION', '${VERSION}')
//                        redefinedProp('CUSTOM_PORT', '$CUSTOM_PORT')
                    }
                }
            }
        }
    }

}

def KISTERS_DOCKER_HOME = "/opt/kisters/docker"
def BUILD_URL = "https://jenkins.energy-dev.kisters.de/job/\${JOB_NAME}/\${BUILD_NUMBER}/console"
def EMAIL_TO = "Polina.Mrachkovskaya@kisters.de"


job('deploy-staging') {
    label null
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
        shell("""
rm -f .env
printf \"VERSION=\${VERSION}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'rm -rf $KISTERS_DOCKER_HOME/test && mkdir $KISTERS_DOCKER_HOME/test'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-test:$KISTERS_DOCKER_HOME/test
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-test:$KISTERS_DOCKER_HOME/test
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test "cd $KISTERS_DOCKER_HOME/test && docker-compose up -d \$MS"
""")
    }
    publishers {
        downstreamParameterized {
            trigger('test-staging') {
                parameters {
                    predefinedProp('VERSION', '${VERSION}')
                    predefinedProp('MS', '$MS')
//                    redefinedProp('CUSTOM_PORT', '${CUSTOM_PORT}')
                }
            }
        }
    }
}


job('test-staging') {
    label null

    wrappers {
        credentialsBinding {
            file('test', 'remote-deploy')
        }
    }
    steps {
        shell("""
sleep 4
echo "\${CUSTOM_PORT}"
statusCode=\$(curl -sL -w '%{http_code}' 'http://en-cdeval-test:8081/test?country=Aus' -o /dev/null)
if [ "\$statusCode" -ne "200" ]; then 
    exit 1 
fi
""")
    }
    publishers {
        extendedEmail {
            recipientList(EMAIL_TO)
            defaultSubject('Jenkins $JOB_NAME')
            defaultContent(BUILD_URL)
            contentType('text/html')
            triggers {
                success{
                    attachBuildLog(true)
                    sendTo {
                        recipientList()
                    }
                }

            }
        }
        buildPipelineTrigger('deploy-prod'){
            parameters {
                predefinedProp('VERSION', '${VERSION}')
                predefinedProp('MS', '${MS}')
//                redefinedProp('CUSTOM_PORT', '${CUSTOM_PORT}')
            }
        }
        downstreamParameterized {
            trigger('deploy-demo') {
                parameters {
                    predefinedProp('VERSION', '${VERSION}')
                    predefinedProp('MS', '$MS')
//                    redefinedProp('CUSTOM_PORT', '${CUSTOM_PORT}')
                }
            }
        }
    }
}
job('deploy-demo') {
    label null
    steps {
        shell('sleep 5 && echo DEMO && echo building ${MS} with version ${VERSION}')
    }
    publishers {
        downstream('finish', "SUCCESS")
    }
}

job('deploy-prod') {
    label null

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
        shell("""
if  [ "\$OPTION" = "stop" ]; then
    echo "The pipeline was stopped intentionally. The user did not want to deploy to production."
    exit 0
fi
echo \$OPTION
rm -f .env
printf \"VERSION=\${VERSION}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'rm -rf $KISTERS_DOCKER_HOME/test && mkdir $KISTERS_DOCKER_HOME/test'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-prod:$KISTERS_DOCKER_HOME/test
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-prod:$KISTERS_DOCKER_HOME/test
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod "cd $KISTERS_DOCKER_HOME/test && docker-compose up -d \$MS"
""")

    }
    publishers {
        downstream('finish', "SUCCESS")
    }


}
job('finish'){
    blockOnUpstreamProjects()
    steps{
        shell('echo FINISH')
    }
}

nestedView('Seminar-Pipelines') {
    views {
        buildPipelineView('ms1-commit pipeline') {
            displayedBuilds(5)
            selectedJob('ms1-build-app')
            alwaysAllowManualTrigger()
            showPipelineParameters()
        }
        buildPipelineView('ms2-commit pipeline') {
            displayedBuilds(5)
            selectedJob('ms2-build-app')
            alwaysAllowManualTrigger()
            showPipelineParameters()
        }


    }
}

