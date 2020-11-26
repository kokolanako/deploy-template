job("example"){
    scm{
        git('git://github.com/wardviaene/docker-demo.git'){
            node->
                node / gitConfigName("DSL User")
        }

    }
    steps{
        shell('ls -la')
    }
}