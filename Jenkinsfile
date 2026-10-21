pipeline {
  agent any
  tools { jdk 'jdk21' }
  stages {
    stage('Build and test') { steps { sh 'chmod +x mvnw && ./mvnw -B verify' } }
    stage('Reports') { steps { junit 'target/surefire-reports/*.xml'; archiveArtifacts artifacts: 'target/*.war', fingerprint: true } }
  }
  post { always { cleanWs() } }
}
