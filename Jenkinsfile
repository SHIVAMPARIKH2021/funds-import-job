pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    parameters {
        choice(name: 'YEAR', choices: ['2026', '2025', '2024', '2023'], description: 'Target filing year for the batch job')
        choice(name: 'QUARTER', choices: ['1', '2', '3', '4'], description: 'Target filing quarter for the batch job')
        string(name: 'EXECUTOR', defaultValue: 'System User', description: 'Audit tracking user')
        booleanParam(name: 'FORCE_DOCKER_NO_CACHE', defaultValue: false, description: 'Force Docker to rebuild layers from scratch on Windows host')
    }

    environment {
        GIT_REPO        = 'https://github.com/SHIVAMPARIKH2021/funds-import-job.git'
        DOPPLER_TOKEN   = credentials('doppler-personal-token')
        DOPPLER_PROJECT = 'sec-pipeline'
        DOPPLER_CONFIG  = 'prd'
        SSH_CREDS_ID    = 'windows-laptop-ssh'
        IMAGE_BASE_NAME = 'funds-import-job'
    }

    stages {
        stage('1. Fetch Secrets from Doppler Cloud') {
            steps {
                script {
                    def response = sh(
                        script: '''
                            curl -s --request GET \
                              --url "https://api.doppler.com/v3/configs/config/secrets/download?project=${DOPPLER_PROJECT}&config=${DOPPLER_CONFIG}&format=json" \
                              --header "accept: application/json" \
                              --header "authorization: Bearer ${DOPPLER_TOKEN}"
                        ''',
                        returnStdout: true
                    ).trim()

                    def secrets = readJSON text: response

                    env.WINDOWS_HOST                = secrets.WINDOWS_HOST
                    env.WINDOWS_USER                = secrets.WINDOWS_USER
                    env.SPRING_DATASOURCE_URL       = secrets.SPRING_DATASOURCE_URL ?: "jdbc:postgresql://host.docker.internal:5432/funds?sslmode=disable"
                    env.SPRING_DATASOURCE_USERNAME  = secrets.SPRING_DATASOURCE_USERNAME ?: secrets.DB_USER ?: "funds_admin"
                    env.SPRING_DATASOURCE_PASSWORD  = secrets.SPRING_DATASOURCE_PASSWORD ?: secrets.DB_PASSWORD ?: "strong/?12x3401"
                    env.BENCHMARK_REGEX             = secrets.BENCHMARK_REGEX ?: "\\s*\\(reflects no deduction for [^)]*\\)"

                    echo "Doppler secrets loaded successfully for host: ${env.WINDOWS_HOST}"
                }
            }
        }

        stage('2. Checkout & Local Test Verification') {
            steps {
                checkout scm
                script {
                    sh '''
                        chmod +x ./gradlew
                        ./gradlew clean test --no-daemon --stacktrace
                    '''
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    echo "Deploying commit: ${env.GIT_COMMIT_SHORT}"
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('3. Stream Context & Build Docker Image on Windows') {
            steps {
                script {
                    def noCacheFlag = params.FORCE_DOCKER_NO_CACHE ? "--no-cache" : ""
                    withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDS_ID, keyFileVariable: 'SSH_KEY')]) {
                        sh """
                            tar -czf - --exclude='.git' --exclude='build' . | ssh -i "\$SSH_KEY" \
                                -o StrictHostKeyChecking=no \
                                -o ConnectTimeout=15 \
                                ${WINDOWS_USER}@${WINDOWS_HOST} \
                                "docker build ${noCacheFlag} \
                                    -t ${IMAGE_BASE_NAME}:latest \
                                    -t ${IMAGE_BASE_NAME}:${GIT_COMMIT_SHORT} -"
                        """
                    }
                }
            }
        }

        stage('4. Execute Spring Batch Job in Container') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDS_ID, keyFileVariable: 'SSH_KEY')]) {
                        sh """
                            set -e
                            ssh -i "\$SSH_KEY" \
                                -o StrictHostKeyChecking=no \
                                -o ConnectTimeout=15 \
                                ${WINDOWS_USER}@${WINDOWS_HOST} \
                                "docker run --rm \
                                    --add-host=host.docker.internal:host-gateway \
                                    -e SPRING_PROFILES_ACTIVE=prod \
                                    -e SPRING_DATASOURCE_URL='${SPRING_DATASOURCE_URL}' \
                                    -e SPRING_DATASOURCE_USERNAME='${SPRING_DATASOURCE_USERNAME}' \
                                    -e SPRING_DATASOURCE_PASSWORD='${SPRING_DATASOURCE_PASSWORD}' \
                                    -e BENCHMARK_REGEX='${BENCHMARK_REGEX}' \
                                    -e JAVA_OPTS='-Xms256m -Xmx1024m' \
                                    ${IMAGE_BASE_NAME}:${GIT_COMMIT_SHORT} \
                                    year=${params.YEAR} \
                                    quarter=${params.QUARTER} \
                                    executor='${params.EXECUTOR}'"
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Funds Import Job pipeline completed successfully for commit ${env.GIT_COMMIT_SHORT}."
        }
        failure {
            echo "Funds Import Job pipeline failed. Inspect the logs above."
        }
    }
}