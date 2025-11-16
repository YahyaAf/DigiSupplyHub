pipeline {
    agent any

    tools {
       jdk 'jdk17'
       maven 'maven3'
    }

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')  // Après création des credentials
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
                sh 'mvn test jacoco:report'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                script {
                    // Essaie d'abord host.docker.internal, puis l'IPs
                    sh '''
                        echo "Trying host.docker.internal..."
                        mvn sonar:sonar \
                          -Dsonar.projectKey=digital-logistics \
                          -Dsonar.projectName=digital-logistics \
                          -Dsonar.host.url=http://host.docker.internal:9000 \
                          -Dsonar.token=sqp_f995d0632d6a880ddd01a53e7e1500500ebb606a \
                        || (
                          echo "First method failed, trying with IP..."
                          mvn sonar:sonar \
                            -Dsonar.projectKey=digital-logistics \
                            -Dsonar.projectName=digital-logistics \
                            -Dsonar.host.url=http://192.168.1.100:9000 \
                            -Dsonar.token=sqp_f995d0632d6a880ddd01a53e7e1500500ebb606a
                        )
                    '''
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
        }
        success {
            echo 'Pipeline exécuté avec succès! 🎉'
        }
        failure {
            echo 'Pipeline a échoué! ❌'
        }
    }
}