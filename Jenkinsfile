pipeline {
        agent any

        tools {
           jdk 'jdk17'          // Jenkins JDK tool ID
           maven 'maven3'       // Jenkins Maven tool ID
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('Build') {
                steps {
                    sh 'mvn clean compile'
                }
            }

            stage('Unit Tests') {
                steps {
                    sh 'mvn test'
                }
                post {
                    always {
                       junit 'target/surefire-reports/*.xml'
                    }
                }
            }

            stage('Package') {
                steps {
                    sh 'mvn package -DskipTests'
                    archiveArtifacts artifacts: 'target/.jar', fingerprint: true
                }
            }
        }

        post {
            always {
                echo "Build ${currentBuild.result} - ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            }
            success {
                echo 'Pipeline exécuté avec succès!'
            }
            failure {
                echo 'Pipeline a échoué!'
            }
        }
    }