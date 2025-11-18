pipeline {
    agent any

    tools {
       jdk 'jdk17'
       maven 'maven3'
    }

    environment {
        SONAR_HOST_URL = 'http://localhost:9000'
        SONAR_TOKEN = credentials('sonar-token')
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
                sh '''
                    mvn test jacoco:report \
                    -Daws.accessKeyId=$AWS_ACCESS_KEY_ID \
                    -Daws.secretKey=$AWS_SECRET_ACCESS_KEY \
                    -Daws.region=$AWS_REGION \
                    -Daws.s3.bucket-name=$S3_BUCKET \
                    -Daws.s3.bucket=$S3_BUCKET  # ← Ajouter cette ligne
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
                withCredentials([string(credentialsId: 'sonar-token', variable: 'REAL_SONAR_TOKEN')]) {
                   sh """
                      mvn sonar:sonar \\
                         -Dsonar.projectKey=digital-logistics \\
                         -Dsonar.projectName=digital-logistics \\
                         -Dsonar.host.url=http://my-sonarqube:9000 \\
                         -Dsonar.token=$REAL_SONAR_TOKEN
                      """
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Upload to S3') {
            steps {
                sh """
                   aws s3 cp target/digital-logistics-*.jar s3://$S3_BUCKET/artifacts/${BUILD_NUMBER}/ --region $AWS_REGION
                """
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