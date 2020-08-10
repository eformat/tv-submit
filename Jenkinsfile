pipeline {

    agent {
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        NAME = "tv-submit"

        // Job name contains the branch eg my-app-feature%2Fjenkins-123
        JOB_NAME = "${JOB_NAME}".replace("%2F", "-").replace("/", "-")
        IMAGE_REPOSITORY= 'image-registry.openshift-image-registry.svc:5000'

        GIT_REPO = "https://github.com/eformat/sc-submit.git#${GIT_BRANCH}"
        GIT_SSL_NO_VERIFY = true

        // Credentials bound in OpenShift
        GIT_CREDS = credentials("${PIPELINES_NAMESPACE}-git-auth")
        NEXUS_CREDS = credentials("${PIPELINES_NAMESPACE}-nexus-password")
        ARGOCD_CREDS = credentials("${PIPELINES_NAMESPACE}-argocd-token")

        // Nexus Artifact repo
        NEXUS_REPO_NAME="labs-static"
        NEXUS_REPO_HELM = "helm-charts"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('Perpare Environment') {
            failFast true
            parallel {
                stage("Release Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("master") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            env.STAGING_NAMESPACE = "labs-staging"
                            env.APP_NAME = "${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
                stage("Sandbox Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("dev") || GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("fix") || GIT_BRANCH.startsWith("nsfw") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            // ammend the name to create 'sandbox' deploys based on current branch
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
                stage("Pull Request Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("PR-") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
            }
        }

        stage("Build") {
            parallel {
                stage("Build App") {
                    agent {
                        node {
                            label "jenkins-agent-mvn"
                        }
                    }
                    steps {
                        script {
                            env.VERSION = readMavenPom().getVersion()
                            env.PACKAGE = "${NAME}-${VERSION}-runner.jar"
                            env.JAVA_HOME = "/usr/lib/jvm/java-11-openjdk"
                        }

                        script {
                            echo '### Running checkstyle ###'
                            // sh 'mvn checkstyle:check'

                            echo '### Running tests ###'
                            // sh 'mvn test'

                            echo '### Running build ###'
                            sh '''                            
                            mvn package -DskipTests -s ocp/settings.xml
                            
                            oc get bc ${APP_NAME} || rc=$?
                            if [ $rc -eq 1 ]; then
                                echo " 🏗 no build - creating one 🏗"
                                oc new-build --binary --name=${APP_NAME} -l app=${APP_NAME} --strategy=docker --dry-run -o yaml > /tmp/bc.yaml
                                yq w -i /tmp/bc.yaml items[1].spec.strategy.dockerStrategy.dockerfilePath Dockerfile.jvm
                                oc apply -f /tmp/bc.yaml
                                oc patch bc/${APP_NAME} -p '{"spec":{ "runPolicy": "Parallel"}}' --type=strategic
                            fi
                            echo " 🏗 build found - starting it  🏗"
                            oc start-build ${APP_NAME} --from-dir=. --follow                                                        
                            '''
                        }
                    }
                }
            }
        }

        stage ("Deploy") {
            agent {
                node {
                    label "master"
                }
            }
            steps {
                script {
                    sh '''
                       oc tag ${APP_NAME}:latest ${TARGET_NAMESPACE}/${APP_NAME}:latest
                       oc -n ${TARGET_NAMESPACE} get dc ${APP_NAME} || rc=$?
                       if [ $rc -eq 1 ]; then
                            echo " 🏗 no deployment found - creating 🏗"
                            oc -n ${TARGET_NAMESPACE} create -f ocp/openshift-deployment.yml
                       fi
                       echo " 🏗 found pod waiting for deployment 🏗"                       
                       oc -n ${TARGET_NAMESPACE} wait dc -l app=${APP_NAME} --for=condition=Available --timeout=300s                        
                    '''
                }
            }
        }
    }
}
