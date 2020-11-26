job("MS1-checkout"){
    parameters{
        nodeParam('build-slave-maven')
    }
    scm{
        git{
            remote {
               url ('https://github.com/kokolanako/be.git')
            }
            branch('ms1')
        }

    }
    steps{
        shell('ls -la')
    }
}