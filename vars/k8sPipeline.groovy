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
        parameters {
            choice(name: 'sonarScans',
                choices: 'no\nyes',
                description: 'This will scan the applicaiton using sonar'
            )
            choice(name: 'buildOnly',
                choices: 'no\nyes',
                description: 'This will only build the application'
            )
            choice(name: 'dockerPush',
                choices: 'no\nyes',
                description: "This will trigger the build, docker build and docker push"
            )
            choice(name: 'deployToDev',
                choices: 'no\nyes',
                description: "This will Deploy my app to Dev env"
            )
            choice(name: 'deployToTest',
                choices: 'no\nyes',
                description: "This will Deploy my app to Test env"
            )
            choice(name: 'deployToStage',
                choices: 'no\nyes',
                description: "This will Deploy my app to Stage env"
            )
            choice(name: 'deployToProd',
                choices: 'no\nyes',
                description: "This will Deploy my app to Prod env"
            )
        }
        environment {
            APPLICATION_NAME = "${pipelineParams.appName}"
            CLUSTER_NAME = "i27"
            REGION = "ap-south-1"
            ROLE_ARN = "arn:aws:iam::533267231414:role/eks"
            //APPLICATION_NAME = "eureka"
            SONAR_URL = "http://http://13.126.225.26:9000/:9000"
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
                       k8s.auth_login("${env.CLUSTER_NAME}", "${env.REGION}", "${env.ROLE_ARN}") 
                    }
                }
            }

 
      } 