@Library('gematik-jenkins-shared-library') _

def BRANCH = 'main'
def RELEASE_BRANCH = 'release'
def RELEASE_VERSION = '1.0.27'
def PROJECT_NAME = 'titus-erx-vau-encrypting-proxy'
def DOCKER_IMAGE = "e-rezept/${PROJECT_NAME}"

pipeline {
    agent { label 'k8-docker' }
    options {
        disableConcurrentBuilds()
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')
    }
    stages {
        stage('Build Docker Image') {
            steps {
                dockerBuild("${DOCKER_IMAGE}", "latest", "latest")
            }
        }
        stage('Release Latest Docker Image') {
            when {
                anyOf {
                    branch BRANCH;
                }
                beforeAgent true
            }
            steps {
                dockerPushImage("${DOCKER_IMAGE}", "latest")
            }
        }
        stage('build and push newest docker images') {  // temporary stage since 'latest' interacts poorly with "roll-out E-Rezept" and deploy-by-portainer currently doesn't work with new Registry
            when {
                anyOf {
                    branch pattern: "BUILD-NEWEST-FROM-.*", comparator: "REGEXP";
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'svc_gitlab_prod_credentials',  usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    dockerBuild("${DOCKER_IMAGE}", "newest", "newest", '--build-arg="CI_JOB_USER=$USERNAME" --build-arg="CI_JOB_TOKEN=$PASSWORD"')
                    dockerPushImage("${DOCKER_IMAGE}", "newest")
                    dockerRemoveLocalImage("${DOCKER_IMAGE}", "newest")
                }
            }
        }
        stage('Release Docker Image') {
            when {
                branch RELEASE_BRANCH
                beforeAgent true
            }
            steps {
                dockerReTagImage("${DOCKER_IMAGE}", RELEASE_VERSION)
                dockerPushImage("${DOCKER_IMAGE}", RELEASE_VERSION)
                sh "git tag ${RELEASE_VERSION} && git push origin refs/tags/${RELEASE_VERSION}"
                dockerRemoveLocalImage("${DOCKER_IMAGE}", RELEASE_VERSION)
            }
        }
    }
    post('Clean up Docker Image') {
        always {
            dockerRemoveLocalImage("${DOCKER_IMAGE}", "latest")
        }
    }
}
