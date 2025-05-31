#!/usr/bin/env groovy

def call(Map config = [:]) {
    // Default configuration
    def defaults = [
        dockerImage: "${config.dockerRepo ?: ''}/${config.appName ?: 'node-app'}",
        dockerTag: "latest",
        testScript: "test",
        buildDir: "."
    ]
    
    // Merge user config with defaults
    config = defaults + config
    
    pipeline {
        agent any
        
        stages {
            stage('Install Dependencies') {
                steps {
                    script {
                        echo "Installing Node.js dependencies..."
                        sh "npm install"
                    }
                }
            }
            
            stage('Run Tests') {
                steps {
                    script {
                        echo "Running tests..."
                        sh "npm run ${config.testScript}"
                    }
                }
            }
            
            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building Docker image..."
                        docker.build("${config.dockerImage}:${config.dockerTag}", "${config.buildDir}")
                    }
                }
            }
            
            stage('Push to DockerHub') {
                steps {
                    script {
                        echo "Pushing to DockerHub..."
                        withCredentials([usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKERHUB_USER',
                            passwordVariable: 'DOCKERHUB_PASS'
                        )]) {
                            sh "docker login -u $DOCKERHUB_USER -p $DOCKERHUB_PASS"
                            sh "docker push ${config.dockerImage}:${config.dockerTag}"
                        }
                    }
                }
            }
        }
        
        post {
            always {
                cleanWs()
            }
        }
    }
}