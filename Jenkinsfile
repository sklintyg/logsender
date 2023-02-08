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

//
// Pipeline
//
pipeline {

    agent any

    environment {
        TEST_MODE = 'true'
        GIT_BRANCH = 'pocbranch'
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


                    // If the job was started manually, ask for the branch to build
                    if ( essJob.isStartedManually()) {
                        buildBranch = essAsk.askForBranch default: gitBranch

                        cloneInfo = essGit.checkoutRef url: gitUrl, branch: buildBranch
                    }

                    buildTag = (gitBranch == buildBranch) ? '' : essJob.getBuildTagFromBranchName( branch: buildBranch)

                    essJob.tagBuildName tag: buildTag

                    commit   = essGit.getCommit()
                    project  = essJob.getProperty( name:'project.name')
                    artifact = essJob.getProperty( name:'artifact.name')
                    version  = essCmn.getVersion()
                    culprits = essGit.getCulpritsMail( info:cloneInfo)
                }
            }
        }

        stage('Build') {
            agent {
              docker {
                image 'gradle:6.9.2-jdk11'
                reuseNode true
              }
            }
            steps {
              sh 'gradle build --no-daemon -PcodeQuality -DbuildVersion=0.1.0  -DinfraVersion=3.19.0.+ -DcommonVersion=3.19.0.+ -DrefDataVersion=1.0-SNAPSHOT -Dfile.encoding=UTF-8'
              sh 'cp ./web/build/libs/*.war ./cicd/ROOT.war'
            }
        }

        stage('Build Image') {
            steps {
                script {
                    essDocker.build( project:project, name:artifact, version:version, commit:commit, path:'cicd', url:gitUrl, tag:buildTag)
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
