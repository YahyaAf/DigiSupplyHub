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
                    -Daws.s3.bucket=$S3_BUCKET
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

        stage('Upload Artifact to S3') {
            steps {
                withAWS(credentials: 'aws-jenkins', region: env.AWS_REGION) {
                    s3Upload(
                        bucket: env.S3_BUCKET,
                        path: "artifacts/${BUILD_NUMBER}/",
                        file: "target/Digital_Logistics-0.0.1-SNAPSHOT.jar"
                    )
                }
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