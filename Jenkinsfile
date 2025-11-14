pipeline {

    agent any

    tools {
        jdk 'jdk17'          // Jenkins JDK tool ID
        maven 'maven3'       // Jenkins Maven tool ID
    }

    environment {
        REPO_URL     = 'git@github.com:YahyaAf/DigiSupplyHub.git'
        CREDENTIALS  = 'github-ssh-yahyaaf'
        BRANCH       = 'main'
    }

    stages {

        /* ────────────────────────────────────────────
           1. Environment Setup
        ───────────────────────────────────────────── */
        stage('Environment Check') {
            steps {
                echo "\u001B[34m🔧 Checking Build Environment...\u001B[0m"
                sh '''
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "Java Version:"
                    java -version
                    echo ""
                    echo "Maven Version:"
                    mvn -version
                    echo ""
                    echo "Git Version:"
                    git --version
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                '''
            }
        }

        /* ────────────────────────────────────────────
           2. Clone Application Repository
        ───────────────────────────────────────────── */
        stage('Clone Repository') {
            steps {
                echo "\u001B[36m📥 Cloning Git Repository via SSH...\u001B[0m"

                git branch: env.BRANCH,
                    credentialsId: env.CREDENTIALS,
                    url: env.REPO_URL

                sh '''
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    echo "📁 Workspace Files:"
                    ls -la
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                '''
            }
        }

        /* ────────────────────────────────────────────
           3. Git Commit Metadata
        ───────────────────────────────────────────── */
        stage('Repository Info') {
            steps {
                echo "\u001B[35m🔍 Extracting Repository Metadata...\u001B[0m"

                sh '''
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    BRANCH=$(git rev-parse --abbrev-ref HEAD)
                    COMMIT=$(git log -1 --pretty=format:"%h")
                    AUTHOR=$(git log -1 --pretty=format:"%an")
                    DATE=$(git log -1 --pretty=format:"%ad")
                    MESSAGE=$(git log -1 --pretty=format:"%s")

                    printf "%-15s %-40s\n" "Key" "Value"
                    printf "%-15s %-40s\n" "--------------" "-----------------------------"
                    printf "%-15s %-40s\n" "Branch" "$BRANCH"
                    printf "%-15s %-40s\n" "Commit" "$COMMIT"
                    printf "%-15s %-40s\n" "Author" "$AUTHOR"
                    printf "%-15s %-40s\n" "Date" "$DATE"
                    printf "%-15s %-40s\n" "Message" "$MESSAGE"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                '''
            }
        }

        /* ────────────────────────────────────────────
           4. Maven Build + Verify
        ───────────────────────────────────────────── */
        stage('Build & Compile') {
            when { expression { fileExists('pom.xml') } }
            steps {
                echo "\u001B[33m⚙️ Running Maven Build (clean install)...\u001B[0m"
                sh '''
                    mvn -ntp clean install -DskipTests
                '''
            }
        }

        /* ────────────────────────────────────────────
           5. Run Unit Tests in Parallel
        ───────────────────────────────────────────── */
        stage('Tests') {
            when { expression { fileExists('pom.xml') } }
            parallel {
                stage('Unit Tests') {
                    steps {
                        echo "\u001B[32m🧪 Running Unit Tests...\u001B[0m"
                        sh 'mvn -ntp test'
                    }
                }

                stage('Code Style Check') {
                    steps {
                        echo "\u001B[32m🔍 Running Code Quality Checks...\u001B[0m"
                        sh '''
                            mvn -ntp checkstyle:check || echo "⚠️ Checkstyle warnings found"
                        '''
                    }
                }
            }
        }

        /* ────────────────────────────────────────────
           6. Package Application Artifact
        ───────────────────────────────────────────── */
        stage('Packaging') {
            when { expression { fileExists('target') } }
            steps {
                echo "\u001B[36m📦 Packaging Application Artifact...\u001B[0m"

                sh '''
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                    ls -lh target/
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                '''
            }
        }
    }

    /* ────────────────────────────────────────────
       POST ACTIONS
    ───────────────────────────────────────────── */
    post {
        success {
            echo "\u001B[32m✅ Pipeline executed successfully!\u001B[0m"
            echo "✔ Git SSH OK"
            echo "✔ Tools OK (JDK + Maven)"
            echo "✔ Build OK"
            echo "✔ Tests OK"
        }

        failure {
            echo "\u001B[31m❌ Pipeline Failed\u001B[0m"
            echo "Check build logs, SSH credentials, or Maven configuration."
        }

        always {
            echo "\u001B[34m📘 Pipeline Completed (Final stage)\u001B[0m"
        }
    }
}
