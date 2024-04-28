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
            // enter the namespace name 
            string (name: 'NAMESPACE_NAME', description: "Enter the name of the kubernetes namespace to be created")
            string (name: 'netpolName', description: "Enter the name of the netpol")
            string (name: 'ADD_PVC', description: "Enter the PVC size u want in your namespace, ex: 1gi", defaultValue: "1")
            booleanParam(name: 'AddNetworkPolicy',
                defaultValue: 'false',
                description: 'Enable this checkbox, if you need a default netpol in your namespace')
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
            //POM_VERSION = readMavenPom().getVersion()
            //POM_PACKAGING = readMavenPom().getPackaging()
            DOCKER_HUB = "docker.io/devopswithcloudhub"
            DOCKER_REPO = "i27eurekaproject"
            USER_NAME = "devopswithcloudhub" // UserID for Dockerhub
            DOCKER_CREDS = credentials('dockerhub_creds')
            DOCKER_IMAGE_TAG = sh(script: 'git log -1 --pretty=%h', returnStdout: true).trim()
            K8S_DEV_FILE = "k8s_dev.yaml"
            HELM_PATH = "${WORKSPACE}/i27-shared-lib/chart"
            DEV_ENV = "dev"
            TST_ENV = "tst"
            NETPOL_PATH = "./i27-shared-lib/src/com/i27academy/k8s/default/netpol-generic.yaml"
            NETPOL_NAME = "${params.netpolName}"
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
            stage('Create Kubernetes Namespaces') {
                steps {
                    script {
                        k8s.namespace_creation("${params.NAMESPACE_NAME}")
                    }
                }
            }
            stage('Manifrest Operations') {
                steps {
                    script {
                        println ("Starting Manifest Operations Stage")
                        if (params.AddNetworkPolicy == true) {
                            println("I am in network policy")
                            if (env.NETPOL_NAME == null) {
                                k8s.netpolReplace(env.NETPOL_PATH, "${params.NAMESPACE_NAME}", "${params.NAMESPACE_NAME}")
                            }
                            else {
                                k8s.netpolReplace(env.NETPOL_PATH, "${params.NAMESPACE_NAME}", "${params.NAMESPACE_NAME}"+'-'+env.NETPOL_NAME)
                            }
                            
                        }
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

