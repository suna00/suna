#! /bin/bash

echo -e "Prepare for Amazon Codedeploy"

export JENKINS_WORKDIR=/var/lib/jenkins/workspace
export JENKINS_PROJ=AWS-CORE
export SPRING_PROFILE=dev

function guide () {
	echo -e "Usage codedeploy-prepare [ jenkins.projectname ]:[ spring.profile.active ]"
}

function splitParams () {
	IFS=":" read -ra PARAMS <<< $1
	JENKINS_PROJ=${PARAMS[0]}
	SPRING_PROFILE=${PARAMS[1]}
}

function mvCodeDeployDir () {
	cp $JENKINS_WORKDIR/$JENKINS_PROJ/src/main/resources/codedeploy/$SPRING_PROFILE/* $JENKINS_WORKDIR/$JENKINS_PROJ/codedeploy	
}

if [ $# -lt 1 ] 
then
	echo "Wrong parameter counts"
	guide
	exit 1
fi

splitParams $1
mvCodeDeployDir

if [ $? -ne 0 ]
then
	echo "Failed to prepare for Amazon Codedeploy"
	exit 1
fi

echo -e "All Files are ready to deploy via Codedeploy"

exit 0