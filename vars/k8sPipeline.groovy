// Test file
import com.i27academy.builds.Docker
import com.i27academy.k8s.K8s

library ('com.i27academy.slb')

def call(Map pipelineParams) {
    Docker docker = new Docker(this)
    K8s k8s = new K8s(this)
        pipeline {
        agent {
            label 'JENKINS-SLAVE'
        } 
        tools {
            maven 'Maven-3.9.6'
            jdk 'JDK-17'
        }
        
        environment {
            APPLICATION_NAME = "${pipelineParams.appName}"
            CLUSTER_NAME = "i27-cloth"
            REGION = "ap-south-1"
            ROLE_ARN = "arn:aws:iam::533267231414:role/eks-cloth"
            //APPLICATION_NAME = "eureka"
            SONAR_URL = "http:/13.234.115.135:9000/"
            // SONAR_TOKEN = "sqa_6c69015b0cd422333397142a660072ec1f4f7fca"
            SONAR_TOKEN = credentials('jenkins')
            POM_VERSION = readMavenPom().getVersion()
            POM_PACKAGING = readMavenPom().getPackaging()
            DOCKER_HUB = "docker.io/vramu2609"
            DOCKER_REPO = "i27eurekaproject"
            USER_NAME = "vramu2609" // UserID for Dockerhub
            DOCKER_CREDS = credentials('DOCKER_CREDS')
            DOCKER_IMAGE_TAG = sh(script: 'git log -1 --pretty=%h', returnStdout: true).trim()
            K8S_DEV_FILE = "k8s_dev.yaml"
        }
      stages {
        stage('Authenticate to aws Cloud') {
                steps {
                   sh "kubectl get nodes"
                   
                }
            }
        stage('Build'){
            steps {
                    script {
                        //buildApp().call()
                        echo "****** Printing Addition Mehthod*******"
                        
                        docker.buildApp("${env.APPLICATION_NAME}")
                    }

                }
        }     
        stage('Unit Tests'){

        steps {
                    echo "Performing Unit Tests for ${env.APPLICATION_NAME} application"
                    sh "mvn test"
              }
        }
         stage ('sonar') {
                
                steps {
                    echo "Starting SonarScan with quality gate"
                    withSonarQubeEnv('SonarQube') {
                        sh """
                            mvn clean verify sonar:sonar \
                                -Dsonar.projectKey=i27-${env.APPLICATION_NAME} \
                                -Dsonar.host.url=${env.SONAR_URL} \
                                -Dsonar.login=${env.SONAR_TOKEN}
                        """
                    }
                    timeout (time: 2, unit: 'MINUTES'){ // NANOSECONDS, ****
                        script {
                            waitForQualityGate abortPipeline: true
                        }
                    } 
                }
            }
      } 
        }
}