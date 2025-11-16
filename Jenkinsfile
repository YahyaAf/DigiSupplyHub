pipeline {
    agent any

    tools {
       jdk 'jdk17'
       maven 'maven3'
    }

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')  // À créer dans Jenkins
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

        stage('Unit Tests & Coverage') {
            steps {
                sh 'mvn test jacoco:report'  // Génère le rapport JaCoCo
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Coverage Report'
                    ])
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Vérifie que SonarQube est disponible
                    sh "curl -f ${SONAR_HOST_URL} || echo 'SonarQube non accessible'"

                    // Exécute l'analyse SonarQube
                    sh "mvn sonar:sonar -Dsonar.projectKey=digital-logistics -Dsonar.projectName=digital-logistics -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.token=${SONAR_TOKEN}"
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            echo "Build ${currentBuild.result} - ${env.JOB_NAME} #${env.BUILD_NUMBER}"
            sh 'ls -la target/ || echo "No target directory"'
        }
        success {
            echo 'Pipeline exécuté avec succès! 🎉'
            // Notification optionnelle
            emailext (
                subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: "L'analyse SonarQube et JaCoCo sont terminées avec succès. Consultez les rapports: ${env.BUILD_URL}",
                to: "ton-email@example.com"
            )
        }
        failure {
            echo 'Pipeline a échoué! ❌'
        }
    }
}