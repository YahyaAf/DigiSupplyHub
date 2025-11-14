pipeline {
    agent any

    tools {
       jdk 'jdk17'
       maven 'maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                retry(3) {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        extensions: [
                            [$class: 'CloneOption', timeout: 60],
                            [$class: 'CheckoutOption', timeout: 30]
                        ],
                        userRemoteConfigs: [[
                            url: 'https://github.com/YahyaAf/DigiSupplyHub.git',
                            credentialsId: 'github-ssh-yahyaaf'
                        ]]
                    ])
                }
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
                    junit 'target/surefire-reports/*.xml'  // تم التصحيح هنا
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true  // تم التصحيح هنا
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