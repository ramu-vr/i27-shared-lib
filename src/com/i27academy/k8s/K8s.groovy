package com.i27academy.k8s

class K8s {
    def jenkins 
    K8s(jenkins) {
        this.jenkins = jenkins
    }

    def auth_login(clusterName, region, roleARN) {
    jenkins.sh """#!/bin/bash
    
    echo "Entering Auth Method for EKS Login"
   
    kubectl get nodes
    """
}

    def k8sdeploy(fileName, docker_image) {
        jenkins.sh """#!/bin/bash
        echo "Executing K8S Deploy Method"
        echo "Final image tag is $docker_image"
        sed -i "s|DIT|$docker_image|g" ./.cicd/$fileName
        kubectl apply -f ./.cicd/$fileName
        """
    }
    def k8sHelmChartDeploy(appName, env, repo, imageTag, helmChartPath) {
        jenkins.sh """#!/bin/bash
        echo "********************* Helm Groovy Method from Groovy *********************"
        # Check if helm chart exists
        if helm list | grep -q "${appName}-${env}-chart"; then 
        echo "helm chart exists!!!!!"
        echo "Upgrading the chart"
        helm upgrade ${appName}-${env}-chart -f ${repo}/.cicd/k8s/values_${env}.yaml --set image.tag=${imageTag} ${helmChartPath}
        else 
        echo "Unable to find the chart"
        echo "Installing the chart"
        helm install ${appName}-${env}-chart -f ${repo}/.cicd/k8s/values_${env}.yaml --set image.tag=${imageTag} ${helmChartPath}
        fi
        """
    }
    def gitClone(creds) {
        jenkins.sh """#!/bin/bash
        echo "********************* Entering Git Clone Method from Groovy *********************"
        git clone -b master https://${creds}@github.com/devopswithcloud/i27-shared-lib.git
        ls -la
        echo "Showing files under i27-shared-lib"
        ls -la i27-shared-lib
        echo "Showing files under src folder"
        ls -la i27-shared-lib/src/com/i27academy/k8s/default/
        """
    }
    def namespace_creation(namespace_name) { //hello-world
       jenkins.sh """#!/bin/bash
        # this method is to create kubernetes namespaces in Our Cluster
        # Verify if kubernetes namespace exist 
        # If exists, skip creation
        # if namespace doesnot exists, create it

        #namespace_name="boutique-dev" # this is our namespace

        # Validate if the namespace name is empty
        if [ -z "${namespace_name}" ]; then 
            echo "Error: Namesnapce cant be emtpy"
            exit 1
        fi
        # Verify if kubernetes namespace exist 
        if kubectl get namespace "${namespace_name}" &> /dev/null; then
            echo "Your Kubernetes namespace '${namespace_name}' exists"
            exit 0
        else 
            echo "Your namespace ${namespace_name} doesnot exists, so createing it!!!!!!!!!!!!!"
            if kubectl create namespace "${namespace_name}" &> /dev/null; then
                echo "Your namespace '${namespace_name}' has created sucessfully"
                exit 0
            else 
                echo "Some error, failed to create namespace '${namespace_name}'"
                exit 1
            fi
        fi

       """
    }
    def netpolReplace(filename, namespace, replace_netpol_name) {
        jenkins.sh """#!/bin/bash
        fname="${filename}"
        echo "this is from netPol replace groovy method"
        echo \${fname}
        sed -i 's/network-allow/${replace_netpol_name}/' \${fname}
        kubectl apply -f \${fname} -n ${namespace}
        kubectl get netpol -n ${namespace}
        """
    }
}