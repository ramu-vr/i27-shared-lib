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
            SONAR_URL = "http://13.234.115.135:9000"
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
        stage ('Docker Build and Push') {
             steps {
                    script  {
                        dockerBuildandPush().call()
                    }
                }
        }
        stage ('Deploy to Dev') { //5761
               
                steps {
                script {
                    imageValidation().call()
                    // docker.io/devopswithcloudhub/i27eureka:tagname
                    def docker_image = "${env.DOCKER_HUB}/${env.DOCKER_REPO}:${env.DOCKER_IMAGE_TAG}"
                    k8s.k8sdeploy("${env.K8S_DEV_FILE}", docker_image)
                    // The below line is for Deployment using Docker
                    //dockerDeploy('dev', '5761', '8761').call()
                    // Kubernetes Deployment
                    //k8s.auth_login("${env.GKE_CLUSTER_NAME}", "${env.GKE_ZONE}", "${env.GKE_PROJECT}")
                    //k8s.deploy()
                    //sh "rm -rf ~/.kube/config"

                }
                }
            }
      } 
        }
}
def dockerBuildandPush() {
    return {
        
        sh "cp ${workspace}/target/i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} ./.cicd"
        echo "listing files in .cicd folder"
        sh "ls -la ./.cicd"
        echo "******************** Building Docker Image ********************"
        
        sh "docker build --force-rm --no-cache --pull --rm=true --build-arg JAR_SOURCE=i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} --build-arg JAR_DEST=i27-${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}.${env.POM_PACKAGING} \
            -t ${env.DOCKER_HUB}/${env.DOCKER_REPO}:${env.DOCKER_IMAGE_TAG} ./.cicd"
        
        echo "******************** Logging to Docker Registry ********************"
        sh "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW}"
        sh "docker push ${env.DOCKER_HUB}/${env.DOCKER_REPO}:${env.DOCKER_IMAGE_TAG}"
    }
}
def imageValidation() {
    return {
        println("Pulling the Docker image")
        try {
            sh "docker pull ${env.DOCKER_HUB}/${env.DOCKER_REPO}:${env.DOCKER_IMAGE_TAG}"
            println ("Pull Success,!!! Deploying !!!!!") 
        }
        catch (Exception e) {
            println("OOPS, Docker image with this tag is not available")
            println("So, Building the app, creating the image and pushing to registry")
            //buildApp().call()
            docker.buildApp("${env.APPLICATION_NAME}")
            dockerBuildandPush().call()
        }
    }
}