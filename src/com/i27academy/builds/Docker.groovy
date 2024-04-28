package com.i27academy.builds


class Docker {
    def jenkins

    Docker(jenkins) {
        this.jenkins = jenkins
    }
    // Addition method 
    def add(firstNumber, secondNumber) {
        //logic
        return firstNumber+secondNumber
    }
    // Application build 
    def buildApp(appName) {
        jenkins.sh """#!/bin/bash
        echo "Building the $appName Applicaiton"
        mvn clean package -DskipTests=true
        """
    }
}