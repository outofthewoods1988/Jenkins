//
//  Author: Hari Sekhon
//  Date: 2021-09-01 12:50:03 +0100 (Wed, 01 Sep 2021)
//
//  vim:ts=2:sts=2:sw=2:et
//
//  https://github.com/HariSekhon/Jenkins
//
//  License: see accompanying Hari Sekhon LICENSE file
//
//  If you're using my code you're welcome to connect with me on LinkedIn and optionally send me feedback to help steer this or other code I publish
//
//  https://www.linkedin.com/in/HariSekhon
//

// must run gcpActiveServiceAccount() first to authenticate
//
// Example call:
//
//    gcpCloudBuild("--project mycompany-shared --substitutions=\"_REGISTRY=\$GCR_REGISTRY,_IMAGE_VERSION=\$GIT_COMMIT,_GIT_BRANCH=\${GIT_BRANCH##*/}\"")
//
// If the following are set then will check GCR for an existing image to skip building entirely
//
//    DOCKER_IMAGE
//    DOCKER_TAG
//
// You may want to set them like this in the environment{} section of your Jenkinsfile:
//
//    DOCKER_IMAGE = "$GCR_REGISTRY/$GCR_PROJECT/myapp"
//    DOCKER_TAG = "${env.GIT_COMMIT.split('/')[-1]}"

def call(args, timeoutMinutes=60){
  echo "Building from branch '${env.GIT_BRANCH}' for '" + "${env.ENVIRONMENT}".capitalize() + "' Environment"
  milestone ordinal: 10, label: "Milestone: Build"
  echo "Running Job '${env.JOB_NAME}' Build ${env.BUILD_ID} on ${env.JENKINS_URL}"
  int timeoutSeconds = timeoutMinutes * 60
  retry(2){
    timeout(time: "$timeoutMinutes", unit: 'MINUTES') {
      gcpActivateServiceAccount()
      echo 'Running GCP CloudBuild'
      withEnv(["TIMEOUT_SECONDS=$timeoutSeconds"]) {
        sh """#!/bin/bash
          set -euxo pipefail
          gcloud auth list
          if [ -n "\${DOCKER_IMAGE:-}" ] &&
             [ -n "\${DOCKER_TAG:-}" ] &&
             [ -z "\$(gcloud container images list-tags "\$DOCKER_IMAGE" --filter="tags:\$DOCKER_TAG" --format=text)" ]; then
             :
          else
            gcloud builds submit --timeout "\$TIMEOUT_SECONDS" $args
          fi
        """
      }
    }
  }
}
