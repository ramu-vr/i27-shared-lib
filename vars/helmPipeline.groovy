// Test file
import com.i27academy.builds.Docker
import com.i27academy.k8s.K8s

library ('com.i27academy.slb')

def call(Map pipelineParams) {
    Docker docker = new Docker(this)
    K8s k8s = new K8s(this)
        pipeline {
        agent {
            label 'k8s-slave'
        } 
        tools {
            maven 'Maven-3.8.8'
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
            GKE_CLUSTER_NAME = "cart-cluster"
            GKE_DEV_CLUSTER_NAME = "dev-cluster"
            GKE_TST_CLUSTER_NAME = "tst-cluster"
            GKE_ZONE = "us-central1-a"
            GKE_PROJECT  = "practical-brace-402514"
            //APPLICATION_NAME = "eureka"
            SONAR_URL = "http://54.196.85.126:9000"
            // SONAR_TOKEN = "sqa_6c69015b0cd422333397142a660072ec1f4f7fca"
            SONAR_TOKEN = credentials('sonar_creds')
            POM_VERSION = readMavenPom().getVersion()
            POM_PACKAGING = readMavenPom().getPackaging()
            DOCKER_HUB = "docker.io/devopswithcloudhub"
            DOCKER_REPO = "i27eurekaproject"
            USER_NAME = "devopswithcloudhub" // UserID for Dockerhub
            DOCKER_CREDS = credentials('dockerhub_creds')
            DOCKER_IMAGE_TAG = sh(script: 'git log -1 --pretty=%h', returnStdout: true).trim()
            K8S_DEV_FILE = "k8s_dev.yaml"
            HELM_PATH = "${WORKSPACE}/i27-shared-lib/chart"
            DEV_ENV = "dev"
            TST_ENV = "tst"
        }
        stages {
            stage ('Checkout') {
                steps {
                    println("Checkout: Git Clone for i27Shared lib Starting")
                    script {
                        withCredentials([string(credentialsId: 'github_i27_pat', variable: 'token')]) {
                            // some block
                            k8s.gitClone("${token}")
                        }
                    }
                }
            }
            stage('Authenticate to Google Cloud') {
                steps {
                    echo "Executing in Google Cloud auth Stage"
                    script {
                        k8s.auth_login("${env.GKE_CLUSTER_NAME}", "${env.GKE_ZONE}", "${env.GKE_PROJECT}")
                        //k8s.auth_login("cart-cluster", "us-central1-a", "practical-brace-402514")
                        //k8s.auth_login def auth_login(gke_cluster_name, gke_zone, gke_project){
                    }
                }
            }

            stage ('Build') {
                when {
                    anyOf { 
                        expression {
                            params.dockerPush == 'yes'
                            params.buildOnly == 'yes'
                        }
                    }
                }
                // Build happens here 
                // Only build should happen, no tests should be available
                steps {
                    script {
                        //buildApp().call()
                        echo "****** Printing Addition Mehthod*******"
                        println docker.add(3,4)
                        docker.buildApp("${env.APPLICATION_NAME}")
                    }

                }
            }
            stage ('Unit Tests'){
                when {
                    anyOf {
                        expression {
                            params.buildOnly == 'yes'
                            params.dockerPush == 'yes'
                        }
                    }
                }
                steps {
                    echo "Performing Unit Tests for ${env.APPLICATION_NAME} application"
                    sh "mvn test"
                }
            }
            stage ('sonar') {
                when {
                    anyOf {
                        expression {
                            params.sonarScans == 'yes'
                            //params.buildOnly == 'yes'
                            //params.dockerPush == 'yes'
                        }
                    }
                }
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
            stage ('Docker Build and Push') {
                when {
                    anyOf {
                        expression {
                            params.dockerPush == 'yes'
                        }
                    }
                }
                steps {
                    script  {
                        dockerBuildandPush().call()
                    }
                }
            }
            stage ('Deploy to Dev') { //5761
                when {
                    anyOf {
                        expression {
                            params.deployToDev == 'yes'
                        }
                    }
                }
                steps {
                script {
                    imageValidation().call()
                    // docker.io/devopswithcloudhub/i27eureka:tagname
                    def docker_image = "${env.DOCKER_HUB}/${env.DOCKER_REPO}:${env.DOCKER_IMAGE_TAG}"
                    //k8s.k8sHelmChartDeploy(appName, env, imageTag, helmChartPath)
                    //appName, env, repo, imageTag, helmChartPath)
                    //k8s.auth_login("${env.GKE_CLUSTER_NAME}", "${env.GKE_ZONE}", "${env.GKE_PROJECT}")
                    k8s.k8sHelmChartDeploy("${APPLICATION_NAME}", "${DEV_ENV}", "${WORKSPACE}", "${env.DOCKER_IMAGE_TAG}", "${env.HELM_PATH}")
                    //k8s.k8sdeploy("${env.K8S_DEV_FILE}", docker_image)
                    // The below line is for Deployment using Docker
                    //dockerDeploy('dev', '5761', '8761').call()
                    // Kubernetes Deployment
                    //k8s.auth_login("${env.GKE_CLUSTER_NAME}", "${env.GKE_ZONE}", "${env.GKE_PROJECT}")
                    //k8s.deploy()
                    //sh "rm -rf ~/.kube/config"

                }
                }
            }
            stage ('Deploy to Test') { //6761
                when {
                    anyOf {
                        expression {
                            params.deployToTest == 'yes'
                        }
                    }
                }
                steps {
                script {
                    imageValidation().call()
                    dockerDeploy('test', '6761', '8761').call()

                }
                }
            }
            stage ('Deploy to Stage') { //6761
                when {
                    anyOf {
                        expression {
                            params.deployToStage == 'yes'
                        }
                    }
                }
                steps {
                script {
                    imageValidation().call()
                    dockerDeploy('stage', '7761', '8761').call()
                }
                }
            }
            stage ('Deploy to Prod') { //6761
                when {
                    allOf {
                        anyOf {
                            expression {
                                params.deployToProd == 'yes'
                            }
                        }
                        anyOf {
                            branch 'release/*'
                            // to deploy only tags to prod
                            
                        }
                    }

                }
                steps {
                    timeout(time: 200, unit: 'SECONDS') {
                    input message: "Deploy to ${env.APPLICATION_NAME} ?? ", ok: 'yes', submitter: 'krish'
                    }
                script {
                    imageValidation().call()
                    dockerDeploy('prod', '8761', '8761').call()
                }
                }
            }
            stage ('Clean') {
                steps {
                    //echo "Commenting Out Workspace"
                    cleanWs()
                }
            }

        }
    }



    // 8761 is the container port , we cant change it.
    // if we really want to change , we can change it using -Dserver.port=9090, this will be your container port
    // but we are considering the below host ports 
        // dev === > 5761
        // test ===> 6761
        // stage ===> 7761
        // prod ====> 8761
}

def dockerDeploy(envDeploy, hostPort, contPort){    
    return {
        echo "******************** Deploying to $envDeploy Environment ********************"
        withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            // some block
            // with this creddentials, i need to connect to dev environment 
            // sshpass
            script {
                // Test to Pull the container on the docker server
                sh "sshpass -p '$PASSWORD' -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_server_ip \"docker pull ${env.DOCKER_HUB}/${env.DOCKER_REPO}:$GIT_COMMIT\""
                //sh "sshpass -p '$PASSWORD' -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_server_ip \"***"
                echo "Stop the Container"
                // If we execute the below command it will fail for the first time,, as continers are not availble, stop/remove will cause a issue.
                // we can implement try catch block.
                try {
                    sh "sshpass -p '$PASSWORD' -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_server_ip \"docker stop ${env.APPLICATION_NAME}-$envDeploy\""
                    echo "Removing the Container"
                    sh "sshpass -p '$PASSWORD' -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_server_ip \"docker rm ${env.APPLICATION_NAME}-$envDeploy\""
                } catch(err) {
                    echo "Caught the error: $err"
                }
                // Run the container
                sh "sshpass -p '$PASSWORD' -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_server_ip \"docker run --restart always --name ${env.APPLICATION_NAME}-$envDeploy -p $hostPort:$contPort -d ${env.DOCKER_HUB}/${env.DOCKER_REPO}:$GIT_COMMIT\""
                
            }
        }
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
