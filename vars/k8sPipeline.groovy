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
            SONAR_URL = "http://http:/13.234.115.135/:9000/:9000"
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
                    echo "Executing in aws Cloud auth Stage"
                    script {
                       
                       kubectl get nodes
                    }
                }
            }

 
      } 
        }
}