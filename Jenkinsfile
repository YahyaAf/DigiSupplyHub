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

        stage('Upload Artifact to S3') {
            steps {
                withCredentials([
                    string(credentialsId: 'access-key', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'secretKey', variable: 'AWS_SECRET_ACCESS_KEY')
                ]) {
                    sh """
                        aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
                        aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
                        aws configure set region ${env.AWS_REGION}

                        aws s3 cp target/Digital_Logistics-0.0.1-SNAPSHOT.jar \
                           s3://${env.S3_BUCKET}/artifacts/${BUILD_NUMBER}/digital-logistics-${BUILD_NUMBER}.jar
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