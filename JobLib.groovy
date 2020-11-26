job("MS1-checkout"){
    parameters{
        nodeParam('en-jenkins-l-2')
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