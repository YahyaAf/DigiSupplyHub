pipeline {
    agent any

    stages {


        stage('Clone Repository') {
            steps {
                echo 'Cloning Digital Logistics repo via SSH...'

                git branch: 'main',
                    credentialsId: 'github-ssh-yahyaaf',
                    url: 'git@github.com:YahyaAf/DigiSupplyHub.git'

                sh '''
                    echo "Files in workspace:"
                    ls -la

                    echo ""
                    echo "Checking pom.xml:"
                    test -f pom.xml && echo "pom.xml found" || echo "pom.xml not found"
                '''
            }
        }

        stage('Repository Info') {
            steps {
                sh '''
                    echo "Repository information:"
                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

                    if [ -d .git ]; then
                        echo " Current branch: $(git branch --show-current)"
                        echo "Latest commit: $(git log -1 --oneline)"
                        echo "Author: $(git log -1 --pretty=format:'%an')"
                        echo "Date: $(git log -1 --pretty=format:'%ad')"
                    else
                        echo "⚠️ Not a git repository"
                    fi

                    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
                '''
            }
        }
    }

    post {
        success {
            echo '✅ SSH connection working perfectly!'
            echo '🐳 Jenkins Docker → GitHub via SSH: SUCCESS'
        }
        failure {
            echo '❌ SSH connection failed'
            echo '🔍 Check: Credentials, SSH key, GitHub settings'
        }
    }
}