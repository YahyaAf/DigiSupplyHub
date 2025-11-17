pipeline {
    agent any

    tools {
       jdk 'jdk17'
       maven 'maven3'
    }

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')
        // Ajouter les credentials AWS
        AWS_ACCESS_KEY_ID = credentials('access-key')
        AWS_SECRET_ACCESS_KEY = credentials('secretKey')
        AWS_REGION = credentials('region')
        S3_BUCKET = credentials('bucket')
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
                // Passer les variables AWS aux tests
                sh '''
                    mvn test jacoco:report \
                    -Daws.accessKeyId=$AWS_ACCESS_KEY_ID \
                    -Daws.secretKey=$AWS_SECRET_ACCESS_KEY \
                    -Daws.region=$AWS_REGION \
                    -Daws.s3.bucket-name=$S3_BUCKET
                '''
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
                    sh '''
                        echo "Trying host.docker.internal..."
                        mvn sonar:sonar \
                          -Dsonar.projectKey=digital-logistics \
                          -Dsonar.projectName=digital-logistics \
                          -Dsonar.host.url=http://host.docker.internal:9000 \
                          -Dsonar.token=$SONAR_TOKEN \
                        || (
                          echo "First method failed, trying with IP..."
                          mvn sonar:sonar \
                            -Dsonar.projectKey=digital-logistics \
                            -Dsonar.projectName=digital-logistics \
                            -Dsonar.host.url=http://192.168.1.100:9000 \
                            -Dsonar.token=$SONAR_TOKEN
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