def be = 'https://github.com/kokolanako/be.git'
def deploy = 'https://github.com/kokolanako/deploy-template.git'
def l1 = 'build-slave-maven'
def d1 = 'en-compile-stage-docker'
def ms1 = 'ms1'
def registry = '705249/lol'
def image = "705249/lol:${BUILD_NUMBER}"//always seed job number
def registryCredential = 'dockerhub'

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


job('ms1-docker-commit-test') {
    label d1
    steps {
        copyArtifacts("MS1-MVN-BUILD") {
        }

        shell('docker build . -t ' + image)
    }
    publishers {
        downstream('ms1-docker-deploy-test', 'UNSTABLE')

    }
}
job('ms1-docker-deploy-test') {
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
            trigger('ssh-connection') {
                parameters {
                    predefinedProp('VERSION', "${BUILD_NUMBER}")
                    predefinedProp('MS', ms1)
                }
            }
        }
    }

}
job('ssh-connection') {
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
                    predefinedProp('MS', ms1)
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
        }
    }
    steps {

        shell("""
rm -f .env
printf \"VERSION=${BUILD_NUMBER}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'rm -rf $KISTERS_DOCKER_HOME/yay && mkdir $KISTERS_DOCKER_HOME/yay'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-test:$KISTERS_DOCKER_HOME/yay
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-test:$KISTERS_DOCKER_HOME/yay
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-test 'cd $KISTERS_DOCKER_HOME/yay && docker-compose up -d $ms1'
""")

    }
    publishers {
        downstream("test-curl", "SUCCESS")

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
//        downstream("prod-deploy", "SUCCESS")
        buildPipelineTrigger('prod-deploy') {

        }
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
    parameters{
        choiceParam('OPTION',['deploy to prod','stop'])
    }
    wrappers {
        credentialsBinding {
            file('test', 'remote-deploy')
        }
    }
    steps {
        shell('echo $OPTION')

        shell("""
if  [ "\$OPTION" -e "stop"];
then
    exit 0
else
rm -f .env
printf \"VERSION=${BUILD_NUMBER}\" >> .env
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'rm -rf $KISTERS_DOCKER_HOME/yay && mkdir $KISTERS_DOCKER_HOME/yay'
scp -i \$test -o StrictHostKeyChecking=no .env root@en-cdeval-prod:$KISTERS_DOCKER_HOME/yay
scp -i \$test -o StrictHostKeyChecking=no docker-compose.yml root@en-cdeval-prod:$KISTERS_DOCKER_HOME/yay
ssh -i \$test -T -o StrictHostKeyChecking=no root@en-cdeval-prod 'cd $KISTERS_DOCKER_HOME/yay && docker-compose up -d $ms1'
fi
""")

    }


}

