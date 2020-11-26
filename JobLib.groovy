job("example"){
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