// Jenkins declarative pipeline: build -> test -> docker -> scan -> push to ECR -> deploy to EKS
// Prereqs on the agent: JDK17, Maven, Docker, aws-cli v2, kubectl, kustomize, trivy.
// Jenkins credentials: 'aws-creds' (AWS access key binding, ideally an assumed role), 'aws-account-id' (secret text).
pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds(); timeout(time: 45, unit: 'MINUTES') }

  environment {
    AWS_REGION     = 'ap-south-1'
    EKS_CLUSTER    = 'shop-prod'
    AWS_ACCOUNT_ID = credentials('aws-account-id')
    ECR            = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    SERVICES       = 'api-gateway user-service order-service payment-service notification-service'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script { env.TAG = sh(returnStdout: true, script: 'git rev-parse --short=8 HEAD').trim() }   // immutable tag = commit
      }
    }

    stage('Build & Unit Test') {
      steps { sh 'mvn -B clean verify' }
      post { always { junit '**/target/surefire-reports/*.xml' } }
    }

    // Optional quality gate (needs SonarQube server configured in Jenkins)
    // stage('Sonar') { steps { withSonarQubeEnv('sonar') { sh 'mvn -B sonar:sonar' } } }

    stage('Docker Build') {
      steps {
        script {
          def jobs = [:]
          env.SERVICES.split(' ').each { s ->
            jobs[s] = { sh "docker build --build-arg SERVICE=${s} -t ${ECR}/shop/${s}:${TAG} ." }
          }
          parallel jobs
        }
      }
    }

    stage('Image Scan') {
      steps {
        script {
          env.SERVICES.split(' ').each { s ->
            sh "trivy image --exit-code 1 --severity CRITICAL --ignore-unfixed ${ECR}/shop/${s}:${TAG}"
          }
        }
      }
    }

    stage('Push to ECR') {
      when { branch 'main' }
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
          sh 'aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR'
          script { env.SERVICES.split(' ').each { s -> sh "docker push ${ECR}/shop/${s}:${TAG}" } }
        }
      }
    }

    stage('Deploy to Staging') {
      when { branch 'main' }
      steps { deploy('staging') }
    }

    stage('Approve Production') {
      when { branch 'main' }
      steps { input message: 'Deploy to PRODUCTION?', ok: 'Deploy', submitter: 'release-managers' }
    }

    stage('Deploy to Production') {
      when { branch 'main' }
      steps { deploy('prod') }
    }
  }

  post {
    failure { echo "Build ${env.BUILD_NUMBER} failed - notify Slack/email here (slackSend / emailext)" }
    always  { cleanWs() }
  }
}

def deploy(String ns) {
  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-creds']]) {
    sh "aws eks update-kubeconfig --name ${EKS_CLUSTER} --region ${AWS_REGION}"
    dir('k8s') {
      script { env.SERVICES.split(' ').each { s ->
        sh "kustomize edit set image REGISTRY/shop/${s}=${ECR}/shop/${s}:${TAG}" } }
      sh "kubectl apply -k . -n ${ns}"
      script {
        try {
          env.SERVICES.split(' ').each { s -> sh "kubectl rollout status deployment/${s} -n ${ns} --timeout=300s" }
        } catch (err) {
          // automatic rollback when new pods never become Ready
          env.SERVICES.split(' ').each { s -> sh "kubectl rollout undo deployment/${s} -n ${ns} || true" }
          error "Deployment to ${ns} failed and was rolled back: ${err}"
        }
      }
    }
  }
}
