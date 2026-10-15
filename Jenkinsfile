pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        GIT_REPO       = 'https://github.com/SHIVAMPARIKH2021/funds-import-job.git'
        DOPPLER_TOKEN   = credentials('doppler-token')
        DOPPLER_PROJECT = credentials('doppler-project')
        DOPPLER_CONFIG  = credentials('doppler-config')
    }

    stages {
        stage('1. Pull latest code from master') {
            steps {
                deleteDir() // start from a clean workspace
                git branch: 'master', url: "${env.GIT_REPO}"
            }
        }
        stage('2. Gradle clean build') {
            steps {
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean build --no-daemon --stacktrace
                '''
            }
        }
        stage('3. Run job with parameters from Doppler') {
            steps {
                sh '''
                    set -e

                    # 1) Fetch all secrets for the project/config from Doppler
                    curl -s --request GET \\
                        --url "https://api.doppler.com/v3/configs/config/secrets/download?project=${DOPPLER_PROJECT}&config=${DOPPLER_CONFIG}&format=json" \\
                        --header "accept: application/json" \\
                        --header "authorization: Bearer ${DOPPLER_TOKEN}" \\
                        > doppler-secrets.json

                    # Fail fast if Doppler returned an error payload instead of secrets
                    jq -e 'type == "object"' doppler-secrets.json > /dev/null

                    # 2) Convert the Doppler JSON secret map into KEY=value pairs.
                    #    Secret names follow Spring relaxed binding, e.g.
                    #    SPRING_DATASOURCE_URL, QUARTER, YEAR, EXECUTOR, DRYRUN.
                    jq -r 'to_entries[] | "\\(.key)=\\(.value)"' doppler-secrets.json > doppler-secrets.env

                    # 3) Load the secrets into the job environment
                    set -a
                    . ./doppler-secrets.env
                    set +a

                    # 4) Run the Spring Batch job with the boot jar built in stage 2
                    #    (exclude the -plain.jar produced by the plain 'jar' task)
                    BOOT_JAR=$(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)
                    echo "Running job with jar: $BOOT_JAR"
                    java -jar "$BOOT_JAR"
                '''
            }
        }

    }

    post {
        always {
            // Never leave fetched secrets behind in the workspace
            sh 'rm -f doppler-secrets.json doppler-secrets.env'
            junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
        }
        success {
            archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
        }
    }
}