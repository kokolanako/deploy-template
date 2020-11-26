job("MS1-checkout") {
    label 'build-slave-maven' 
    scm {
        git {
            remote {
                url('https://github.com/kokolanako/be.git')
            }
            branch('ms1')
        }

    }
    steps {
        shell('ls -la')
    }
}