#!groovy

properties([
    [
        $class: 'GithubProjectProperty',
        displayName: 'Evidence Management Annotation App',
        projectUrlStr: 'https://github.com/hmcts/em-annotation-app'
    ],
    pipelineTriggers([
        [$class: 'GitHubPushTrigger']
    ]),
    disableConcurrentBuilds()
])

@Library('Reform') _
import uk.gov.hmcts.Ansible
import uk.gov.hmcts.Artifactory
import uk.gov.hmcts.Packager
import uk.gov.hmcts.RPMTagger
import uk.gov.hmcts.Versioner

def channel = '#dm-pipeline'

def app = "em-annotation-app"
def artifactorySourceRepo = "evidence-local"

def ansible = new Ansible(this, 'em')
def artifactory = new Artifactory(this)
def packager = new Packager(this, 'evidence')
def versioner = new Versioner(this)

def rpmTagger
def rpmVersion
def version

node {
    try {

        stage('Checkout') {
            deleteDir()
            checkout scm
        }

        stage('Build') {
            sh "./gradlew clean assemble --info"
        }

        stage('OWasp Dependency Check') {
            try {
                sh "./gradlew dependencyCheckAnalyze -DdependencyCheck.failBuild=false"
            } catch (e) {
                slackSend(
                    channel: channel,
                    color: 'warn',
                    message: "${env.JOB_NAME}:  <${env.BUILD_URL}console|Build ${env.BUILD_DISPLAY_NAME}> has vunerabilities. " +
                        "Check the OWasp report generated by this build for details."
                )
            }
            finally {
                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'application/build/reports',
                    reportFiles          : 'dependency-check-report.html',
                    reportName           : 'OWasp Dependency Report'
                ])
            }
        }

        stage('Test') {
            try {
                sh "./gradlew check --info"
            } finally {
                junit 'application/build/test-results/test/**/*.xml'
                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "application/build/reports/tests/test/",
                    reportFiles          : 'index.html',
                    reportName           : 'Unit Test Report'
                ])

                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "application/build/reports/checkstyle/",
                    reportFiles          : 'main.html',
                    reportName           : 'Checkstyle Main Report'
                ])
                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "application/build/reports/checkstyle/",
                    reportFiles          : 'test.html',
                    reportName           : 'Checkstyle Test Report'
                ])
                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "application/build/reports/pmd/",
                    reportFiles          : 'main.html',
                    reportName           : 'PMD Main Report'
                ])
                publishHTML([
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : "application/build/reports/pmd/",
                    reportFiles          : 'test.html',
                    reportName           : 'PMD Test Report'
                ])

                try {
                    sh './gradlew jacocoTestReport --info'
                }finally {
                    jacoco(execPattern: 'application/build/jacoco/test.exec', buildOverBuild: false,
                        exclusionPattern: '**/test/*, ' +
                            '**/uk/gov/hmcts/reform/em/annotation/EmAnnotationApp.java,'+
                            '**/uk/gov/hmcts/reform/em/annotation/hateos/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/exception/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/domain/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/commandobject/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/hibernate/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/config/**/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/errorhandler/*,'+
                            '**/uk/gov/hmcts/reform/em/annotation/repository/RepositoryFinder.java')
                    publishHTML([
                        allowMissing         : true,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : "application/build/reports/jacoco/test/html/",
                        reportFiles          : 'index.html',
                        reportName           : 'Jacoco Coverage Report'
                    ])
                }
            }
        }

        if ("master" == "${env.BRANCH_NAME}") {
            stage('Sonar') {
                sh "./gradlew sonarqube -Dsonar.host.url=$SONARQUBE_URL"
            }
        }

        stage('Package (JAR)') {
            versioner.addJavaVersionInfo()
            sh "./gradlew installDist bootRepackage"
        }

//        try {
//            stage('Start App with Docker') {
//                sh "docker-compose -f docker-compose.yml -f docker-compose-test.yml pull"
//                sh "docker-compose up --build -d"
//            }
//
//            stage('Run Integration tests in docker') {
//                sh "docker-compose -f docker-compose.yml -f docker-compose-test.yml run -e GRADLE_OPTS em-integration-tests"
//            }
//        }
//        finally {
//            stage('Shutdown docker') {
//                sh "docker-compose logs --no-color > logs.txt"
//                archiveArtifacts 'logs.txt'
//                sh "docker-compose down"
//            }
//        }

        if ("master" == "${env.BRANCH_NAME}") {

            stage('Publish Docker') {
                dockerImage(imageName: "evidence/${app}")
                dockerImage(imageName: "evidence/${app}-database", context: 'docker/database')
            }

            stage('Package (RPM)') {
                rpmVersion = packager.javaRPM(app, 'build/libs/em-annotation-app-$(./gradlew -q printVersion)-all.jar', 'springboot', 'application/src/main/resources/application.yaml')
                version = "{ app: ${app}, rpmversion: ${rpmVersion}}"
            }

            stage('Publish RPM') {
                packager.publishJavaRPM(app)
                rpmTagger = new RPMTagger(this, app, packager.rpmName(app, rpmVersion), artifactorySourceRepo)
            }

            stage ('Deploy on Dev') {
                ansible.run("{}", "dev", "deploy_anno_app.yml")
                rpmTagger.tagDeploymentSuccessfulOn('dev')
            }

//            stage('IT on Dev') {
//                build job: 'evidence/integration-tests-pipeline/master', parameters: [
//                    [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "dev"]
//                ]
//                rpmTagger.tagTestingPassedOn('dev')
//            }

            stage ('Deploy on Test') {
                ansible.run("{}", "test", "deploy_anno_app.yml")
                rpmTagger.tagDeploymentSuccessfulOn('test')
            }

//            stage('IT on Test') {
//                build job: 'evidence/integration-tests-pipeline/master', parameters: [
//                    [$class: 'StringParameterValue', name: 'ENVIRONMENT', value: "test"]
//                ]
//                rpmTagger.tagTestingPassedOn('test')
//            }

            stage('Deploy on Demo') {
                ansible.run("{}", "demo", "deploy_anno_app.yml")
                rpmTagger.tagDeploymentSuccessfulOn('demo')
            }
        }
        notifyBuildFixed channel: channel
    } catch(e) {
        notifyBuildFailure channel: channel
        throw e
    }
}

