@Library( 'essLib')_

//
// Context
//
String gitUrl
String gitBranch
String buildBranch
String buildTag
String project
String artifact
String version
Map    cloneInfo
List   culprits = []
String commit

String infraVersion
String commonVersion
String builderImage
String runtimeImage
String buildArgs

String tagCmd
String pushCmd

//
// Pipeline
//
pipeline {

    agent any

    environment {
        GIT_BRANCH = 'poc/INTYGFV-15255'
    }

    stages {

        stage('Setup') {
            steps {
                script {

                    // Setup environment
                    gitUrl      = essJob.getProperty( name:'git.url', value:essJob.getScmUrl())
                    gitBranch   = essJob.getProperty( name:'git.branch', value:'master')
                    buildBranch = gitBranch
                    buildTag    = ''

                    cloneInfo = essGit.clone url: gitUrl, branch: buildBranch

                    String currentVersion = essCmn.getVersion()
                    String newVersion = essCmn.bumpVersion( version: currentVersion)
                    essCmn.setVersion( version: newVersion)
                    buildTag = 'v' + newVersion

                    essJob.tagBuildName tag: buildTag

                    infraVersion = essJob.getProperty( name:'dependencies.infra.version')
                    commonVersion = essJob.getProperty( name:'dependencies.common.version')
                    builderImage = essJob.getProperty( name:'builder.image')
                    runtimeImage = essJob.getProperty( name:'runtime.image')
                    buildArgs = essJob.getProperty( name:'build.args')

                    commit   = essGit.getCommit()
                    project  = essJob.getProperty( name:'project.name')
                    artifact = essJob.getProperty( name:'artifact.name')
                    version  = essCmn.getVersion()
                    culprits = essGit.getCulpritsMail( info:cloneInfo)

                    commit = essGit.commitChanges( message: 'BUILDENV: Release of ' + buildTag)

                    tagCmd = 'git tag -a ' + buildTag + '-m "Release of ' + buildTag + '"'
                    pushCmd = 'git push --follow-tags --set-upstream origin ' + buildBranch
                }
            }
        }

        stage('Push') {
            environment {
                GIT_AUTH = credentials('intyg-github')
                TARGET_BRANCH = essJob.getProperty( name:'git.branch', value:'master')
            }
            steps {
                sh('''
                    git config --local credential.helper "!f() { echo username=\\$GIT_AUTH_USR; echo password=\\$GIT_AUTH_PSW; }; f"
                    echo ''' + tagCmd + '''
                    echo ''' + pushCmd + '''
                ''')
            }
        }

        stage('Build') {
            when {
                expression { false }
            }
            agent {
              docker {
                image builderImage
                args '-v $HOME/.m2:/root/.m2'
                reuseNode true
              }
            }
            steps {
              sh 'gradle ' + buildArgs + ' --no-daemon -DbuildVersion=' + version + ' -DinfraVersion=' + infraVersion + ' -DcommonVersion=' + commonVersion + ' -Dfile.encoding=UTF-8'
            }
        }

        stage('Build Image') {
            when {
                expression { false }
            }
            steps {
                script {
                    essDocker.build( project:project, name:artifact, version:version, commit:commit, url:gitUrl)
                }
            }
        }

        stage('Notify') {
            steps {
                script {
                    essNotify.success()
                }
            }
        }
    }
}
