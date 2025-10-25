pipeline {
    agent any

    tools {
        maven 'Maven 3.8'
        jdk 'JDK 21'
    }

    environment {
        ORACLE_CREDENTIALS = credentials('oracle-db-credentials')
        JWT_SECRET = credentials('jwt-secret-key')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn verify -DskipUnitTests'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }

        stage('Code Quality Analysis') {
            steps {
                // SonarQube analysis (requires SonarQube server)
                sh 'mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def imageTag = "${env.BUILD_NUMBER}"
                    sh "docker build -t smartwork-api:${imageTag} smartwork-api/"
                    sh "docker tag smartwork-api:${imageTag} smartwork-api:latest"
                }
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'develop'
            }
            steps {
                sh './deploy-staging.sh'
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
                sh './deploy-production.sh'
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded!'
            // Send notification (email, Slack, etc.)
        }
        failure {
            echo 'Pipeline failed!'
            // Send failure notification
        }
        always {
            cleanWs()
        }
    }
}
