#!groovy

def commonVersion = "3.6.0.+"
def infraVersion = "3.6.0.+"

def buildVersion = "6.0.0.${BUILD_NUMBER}"
def buildRoot = JOB_BASE_NAME.replaceAll(/-.*/, "") // Keep everything up to the first dash

stage('checkout') {
    node {
        git url: "https://github.com/sklintyg/logsender.git", branch: GIT_BRANCH
        util.run { checkout scm }
    }
}

stage('build') {
    node {
        try {
            shgradle "--refresh-dependencies clean build testReport sonarqube -PcodeQuality -PcodeCoverage -DgruntColors=false -DbuildVersion=${buildVersion} -DcommonVersion=${commonVersion} -DinfraVersion=${infraVersion}"
        } finally {
            publishHTML allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'build/reports/allTests', \
                reportFiles: 'index.html', reportName: 'JUnit results'
        }
    }
}

stage('tag and upload') {
    node {
        shgradle "uploadArchives tagRelease -DbuildVersion=${buildVersion} -DcommonVersion=${commonVersion} -DinfraVersion=${infraVersion}"
    }
}

stage('notify') {
    node {
        util.notifySuccess()
    }
}
