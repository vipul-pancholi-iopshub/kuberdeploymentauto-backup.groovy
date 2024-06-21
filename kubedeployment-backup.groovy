package jobTemplates


class  KubeDeploymentGHAutoBuild {

    void create(pipelineJob, job_config){
            def env_list = job_config.get_environments()
            def project = job_config.get_project()
            def scan_list = job_config.get_scanning()
            def team = job_config.get_team()
            def vertical = job_config.get_vertical()
            def tenant = job_config.get_tenant()
            def repo_url = job_config.get_repo_url()
            def gh_repo_url = repo_url.replace(".git", "")
            def repo_cred_id = job_config.get_repo_cred_id()
            def APP_NAME = job_config.get_job_name()
            def sonar_url = job_config.get_sonar_url()
            def sonar_internal_url = job_config.get_sonar_internal_url()
            def argocd_host = job_config.get_argocd_host()
            def docker_repo_host = job_config.get_docker_repo_host()
            def dockerfile_path = job_config.get_dockerfile_path()
            def build_stat_endpoint = job_config.get_buildstat_endpoint()
            def prod_endpoint = job_config.get_prod_endpoint()
            def central_stage = ""
            def dev_stage = ""
            def stg_stage = ""
            def prod_stage = ""
            def sonar_scan = ""
            def trivy_scan = ""
            def owasp_scan = ""
            def zap_scan = ""
        if ("central" in env_list) {
            central_stage = """
        stage('Deploy to CENTRAL') {
            // when {
            //     anyOf {
            //         expression{    env.BRANCH == 'main'  }
            //     }
            // }
            steps {
                script{
                    timeout(time: 5, unit: 'MINUTES'){
                        approval = input message: "Move to central", ok: 'Release'
                    }
                }
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime()    
                }
                sh 'rm -rf iopshub-kubernetes-config'
                sh "git clone https://\${GITHUB_CRED_USR}:\${GITHUB_CRED_PSW}@github.com/IOPSHub/iopshub-kubernetes-config"
                dir('iopshub-kubernetes-config') {
                    sh "ls -lrt"
                    sh "sed -i 's|tag: \\".*\\"|tag: \\"\${VERSION}\\"|' accounts/${vertical}/namespaces/${team}/${APP_NAME}/values.yaml"
                    sh "cat accounts/${vertical}/namespaces/${team}/${APP_NAME}/values.yaml"
                    script {
                        sh 'git config --global user.email "build@iopshub.com"'
                        sh 'git config --global user.name "buildsystem"'
                        sh 'git add .'
                        sh 'git commit --allow-empty -m "Update image tag to \${VERSION} using build number \${VERSION}-\${BUILD_NUMBER}"'
                        sh 'git pull origin main'
                        sh 'git push origin main'
                    }
                    container(name: 'argocd') {
                        sh "argocd --grpc-web login ${argocd_host} --username \${ARGO_CRED_USR}  --password \${ARGO_CRED_PSW} --loglevel debug --skip-test-tls"
                        sh "argocd --grpc-web app sync $APP_NAME-central --force"
                        sh "argocd --grpc-web app wait $APP_NAME-central --timeout 600"
                    }
                }
            }
            post {
                success {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#4CAF50;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>CENTRAL</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#4CAF50;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>CENTRAL</div>"
                        }
                        

                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>CENTRAL-X</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>CENTRAL-X</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
        }
        """
        }

        if ("dev" in env_list) {
            dev_stage = """
        stage('Deploy to DEV') {
//            when {
//                anyOf {
//                    expression{    env.BRANCH == 'dev'  }
//                    expression{    env.BRANCH == 'main'  }
//                    expression{    env.BRANCH.startsWith('release')  }    
//                }
////            }
            steps {
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime()    
                }
                sh 'rm -rf iopshub-kubernetes-config'
                sh "git clone https://\${GITHUB_CRED_USR}:\${GITHUB_CRED_PSW}@github.com/IOPSHub/iopshub-kubernetes-config"
                dir('iopshub-kubernetes-config') {
                    sh "ls -lrt"
                    sh "sed -i 's|tag: \\".*\\"|tag: \\"\${VERSION}\\"|' accounts/${vertical}-dev/namespaces/${team}/${APP_NAME}/values.yaml" 
                    sh "cat accounts/${vertical}-dev/namespaces/${team}/${APP_NAME}/values.yaml"
                    script {
                        sh 'git config --global user.email "build@iopshub.com"'
                        sh 'git config --global user.name "buildsystem"'
                        sh 'git add .'
                        
                        sh 'git commit --allow-empty -m "Update image tag to \${VERSION} using build number \${VERSION}-\${BUILD_NUMBER}"'
                        sh 'git pull origin main'
                        sh 'git push origin main'
                    }
                    container(name: 'argocd') {
                        sh "argocd --grpc-web login ${argocd_host} --username \${ARGO_CRED_USR}  --password \${ARGO_CRED_PSW} --loglevel debug --skip-test-tls"
                        sh "argocd --grpc-web app sync $APP_NAME-dev --force"
                        sh "argocd --grpc-web app wait $APP_NAME-dev --timeout 600"
                    }
                }
            } 
            post {
                success {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#808080;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>DEV</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#808080;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>DEV</div>"
                        }

                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on DEV",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>DEV</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>DEV</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on DEV",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
        }
        """
        }

        if ("stg" in env_list) {
            stg_stage = """
        stage('Deploy to STG') {
//            when {
//                anyOf {
//                    expression{    env.BRANCH == 'main'  }
//                    expression{    env.BRANCH.startsWith('release')  }    
//                }
//            }
            steps {                
                script{
                    timeout(time: 5, unit: 'MINUTES'){
                        approval = input message: "Move to STG", ok: 'Release'
                    }
                }
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime()    
                }
                sh 'rm -rf iopshub-kubernetes-config'
                sh "git clone https://\${GITHUB_CRED_USR}:\${GITHUB_CRED_PSW}@github.com/IOPSHub/iopshub-kubernetes-config"
                dir('iopshub-kubernetes-config') {
                    sh "ls -lrt"
                    sh "sed -i 's|tag: \\".*\\"|tag: \\"\${VERSION}\\"|' accounts/${vertical}-stg/namespaces/${team}/${APP_NAME}/values.yaml" 
                    sh "cat accounts/${vertical}-stg/namespaces/${team}/${APP_NAME}/values.yaml" 

                    script {
                        sh 'git config --global user.email "build@iopshub.com"'
                        sh 'git config --global user.name "buildsystem"'
                        sh 'git add .'
                        sh 'git commit --allow-empty -m "Update image tag to \${VERSION} using build number \${VERSION}-\${BUILD_NUMBER}"'
                        sh 'git pull origin main'
                        sh 'git push origin main'
                    }
                    container(name: 'argocd') {
                        sh "argocd --grpc-web login ${argocd_host} --username \${ARGO_CRED_USR}  --password \${ARGO_CRED_PSW} --loglevel debug --skip-test-tls"
                        sh "argocd --grpc-web app sync $APP_NAME-stg --force"
                        sh "argocd --grpc-web app wait $APP_NAME-stg --timeout 600"
                    }
                }
            }
            post {
                success {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#FFA500;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>STG</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#FFA500;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>STG</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on STG",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                 
                
                }
                failure {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>STG-X</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>STG-X</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on STG",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''            
                }
            }
        }
        """
        }

        if ("prod" in env_list) {
            prod_stage = """
        stage('Deploy to PROD') {
            when {
                anyOf {
                    expression{    env.BRANCH == 'main'  } 
                    expression{    env.BRANCH == 'master'  } 
                    expression{    env.BRANCH.startsWith('release')  }      
                }
            }
            steps {
                
                script{
                    timeout(time: 5, unit: 'MINUTES'){
                        approval = input message: "Move to PROD", ok: 'Release', submitter: "paramdeep, saket, arpit, anand, sahej, kartikeya," 
                    }
                }
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime()    
                }
                sh 'rm -rf iopshub-kubernetes-config'
                sh "git clone https://\${GITHUB_CRED_USR}:\${GITHUB_CRED_PSW}@github.com/IOPSHub/iopshub-kubernetes-config"
                dir('iopshub-kubernetes-config') {
                    sh "ls -lrt"
                    sh "sed -i 's|tag: \\".*\\"|tag: \\"\${VERSION}\\"|' accounts/${vertical}-prod/namespaces/${team}/${APP_NAME}/values.yaml" 
                    sh "cat accounts/${vertical}-prod/namespaces/${team}/${APP_NAME}/values.yaml" 
                    script {
                        sh 'git config --global user.email "build@iopshub.com"'
                        sh 'git config --global user.name "buildsystem"'
                        sh 'git add .'
                        sh 'git commit --allow-empty -m "Update image tag to \${VERSION} using build number \${VERSION}-\${BUILD_NUMBER}"'
                        sh 'git pull origin main'
                        sh 'git push origin main'
                    }
                    container(name: 'argocd') {
                        sh "argocd --grpc-web login ${argocd_host} --username \${ARGO_CRED_USR}  --password \${ARGO_CRED_PSW} --loglevel debug --skip-test-tls"
                        sh "argocd --grpc-web app sync $APP_NAME-prod --force"
                        sh "argocd --grpc-web app wait $APP_NAME-prod --timeout 600"
                    }
                }
            }
            post {
                success {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#4CAF50;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>PROD</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#4CAF50;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>PROD</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on PROD",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                            currentBuild.description = currentBuild.description + "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>PROD-X</div>"
                        }
                        else{
                            currentBuild.description = "<div style='padding:2px 4px;border-radius:8px;background-color:#FF0000;color:white;font-size:8px;font-weight:bold;width:50px;text-align:center;'>PROD-X</div>"
                        }
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on PROD",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
        }
        """
        }

        if ("zap" in scan_list) {
            zap_scan = """
        stage('Webapp Security Scan') {
            steps {
                script{
                    def zapStageStartTime= new Date()
                    env.zapStageStartTime = zapStageStartTime.getTime()    
                }
                container(name: 'zap') {
                    sh 'mkdir -p /zap/wrk/'
                    sh 'zap-api-scan.py -t ${prod_endpoint} -f openapi -r report.html -I'
                    // sh 'cp /zap/wrk/report.html .'
                }
                publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: './', reportFiles: 'report.html', reportName: 'Zap Scanning Report', reportTitles: '', useWrapperFileDirectly: true])
                echo "Running Zap Scanning..."
            }
            post {
                success {
                    script{
                        def zapStageEndTime= new Date()
                        println zapStageEndTime.getTime()
                        env.zapStageTime = zapStageEndTime.getTime() - env.zapStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Owasp stage",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def zapStageEndTime= new Date()
                        println zapStageEndTime.getTime()
                        env.zapStageTime = zapStageEndTime.getTime() - env.zapStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                        '''
                }
            }
        }
        """
        }

        if ("owasp" in scan_list) {
            owasp_scan = """
                stage('OSWAP Dependency check') {
                    steps {
                        script{
                    def owaspStageStartTime= new Date()
                    env.owaspStageStartTime = owaspStageStartTime.getTime()    
                }
                        container(name: 'oswap') {
                            sh 'echo scanning for dependency check'
                       }
                    }
                    post {
                success {
                    script{
                        def owaspStageEndTime= new Date()
                        println owaspStageEndTime.getTime()
                        env.owaspStageTime = owaspStageEndTime.getTime() - env.owaspStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Owasp stage",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def owaspStageEndTime= new Date()
                        println owaspStageEndTime.getTime()
                        env.owaspStageTime = owaspStageEndTime.getTime() - env.owaspStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
                }
        """
        }

        if ("sonar" in scan_list) {
            sonar_scan = """
                stage('Static Code Analysis') {   
                    steps {
                        script{
                    def sonarStageStartTime= new Date()
                    env.sonarStageStartTime = sonarStageStartTime.getTime()    
                }
                        container(name: 'sonar-scanner') {
                            echo "Sonarqube Analysis."
                            sh "sonar-scanner -Dsonar.host.url=${sonar_internal_url} -Dsonar.login=sqa_50689ec41d98d261aa23f638d71943d1d4593422 -Dsonar.sources=./ -Dsonar.projectKey=${APP_NAME} -Dsonar.projectName=${APP_NAME} -Dsonar.qualitygate.wait=false"
                            
                            echo "Running Sonar Scanning..."
                        }
                    }
                   post {
                success {
                    script{
                        def sonarStageEndTime= new Date()
                        println sonarStageEndTime.getTime()
                        env.sonarStageTime = sonarStageEndTime.getTime() - env.sonarStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Sonar stage",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def sonarStageEndTime= new Date()
                        println sonarStageEndTime.getTime()
                        env.sonarStageTime = sonarStageEndTime.getTime() - env.sonarStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
                }
        """
        }

        if ("trivy" in scan_list) {
            trivy_scan = """
                stage('File System Scanning') {
                    steps {
                        script{
                    def trivyStageStartTime= new Date()
                    env.trivyStageStartTime = trivyStageStartTime.getTime()
                }
                        container(name: 'trivy') {
                            sh 'trivy fs ./'
                        }
                        echo "Running Trivy Scanning..."
                    } 
                   post {
                success {
                    script{
                        def trivyStageEndTime= new Date()
                        println trivyStageEndTime.getTime()
                        env.trivyStageTime = trivyStageEndTime.getTime() - env.trivyStageStartTime.toLong()
                        
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Trivy stage",
                            "job_status": "pass",
                            "stage_time": "'"\${trivyStageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def trivyStageEndTime= new Date()
                        println trivyStageEndTime.getTime()
                        env.trivyStageTime = trivyStageEndTime.getTime() - env.trivyStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${trivyStageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }           
                }
                 stage('Docker Image Scanning') {   
                    steps {
                        script{
                    def trivyStageStartTime= new Date()
                    env.trivyStageStartTime = trivyStageStartTime.getTime()    
                }
                        container(name: 'trivy') {
                            sh 'TRIVY_USERNAME=\${NEXUS_CRED_USR} TRIVY_PASSWORD=\${NEXUS_CRED_PSW} trivy image --format template --template "@/contrib/html.tpl" -o report.html --exit-code 0 --severity HIGH \"docker.iopshub.com\"/\"buildstat-api\":\\"\${VERSION}\\"'
                        }
                        publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: '/', reportFiles: 'report.html', reportName: 'Docker Scanning', reportTitles: '', useWrapperFileDirectly: true])
                        echo "Running Trivy Scanning..."
                    } 
                   post {
                success {
                    script{
                        def trivyStageEndTime= new Date()
                        println trivyStageEndTime.getTime()
                        env.trivyStageTime = trivyStageEndTime.getTime() - env.trivyStageStartTime.toLong()
                        
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Trivy stage",
                            "job_status": "pass",
                            "stage_time": "'"\${trivyStageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def trivyStageEndTime= new Date()
                        println trivyStageEndTime.getTime()
                        env.trivyStageTime = trivyStageEndTime.getTime() - env.trivyStageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Deploy on central",
                            "job_status": "fail",
                            "stage_time": "'"\${trivyStageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }                  
           }     
        """
        }


        pipelineJob.with {
            description("<h2><a href='${sonar_url}/dashboard?id=${APP_NAME}' target='_blank'>Sonar Dashboard Link</a></h2>")
             properties {
               githubProjectUrl("${gh_repo_url}")
            }
            triggers {
                githubPullRequest {
                    cron('H/5 * * * *')
                    useGitHubHooks()
                    permitAll()
                    extensions {
                        commitStatus {
                            completedStatus('SUCCESS', 'All is well')
                            completedStatus('FAILURE', 'Something went wrong. Investigate!')
                            completedStatus('PENDING', 'still in progress...')
                            completedStatus('ERROR', 'Something went really wrong. Investigate!')
                        }
            }
        }
    }
            definition {
                cps {
                    script("""
pipeline {
    agent {
        kubernetes {
            //label 'kaniko'
            yaml '''        
kind: Pod
metadata:
  labels:
    label: kubeagent
spec:
  containers:
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command:
    - sleep
    args:
    - 99d
  - name: sonar-scanner
    image: sonarsource/sonar-scanner-cli:5.0
    command:
    - sleep
    args:
    - 99d
  - name: argocd
    image: "docker.iopshub.com/base-images/argocd:0.0.1"
    command:
    - sleep
    args:
    - 99d
  - name: trivy
    image: aquasec/trivy
    command:
    - sleep
    args:
    - 99d
  - name: zap
    image: ghcr.io/zaproxy/zaproxy:stable
    command:
    - sleep
    args:
    - 99d
'''
      }
    }
    environment {
        GITHUB_CRED = credentials('iopshub-github')
        REPO_CRED = credentials('iopshub-github')
        NEXUS_CRED = credentials('nexus-admin')                                 
        ARGO_CRED = credentials('argo-admin')
           
    }
    stages {
        stage('checkout'){
            steps {
  
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime()  
                    env.VERSION = "\${env.BUILD_TIMESTAMP}"
                    currentBuild.displayName = "\${env.VERSION}-\${env.BUILD_NUMBER}"
                }
                git branch: "\${GIT_BRANCH}",
                credentialsId: '${repo_cred_id}',
                url: "${repo_url}"
            }
            post {
                success {
                    script{
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Checkout Stage",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Checkout Stage",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
        }
        stage('Build Step') {
            steps {
                script{
                    def stageStartTime= new Date()
                    env.stageStartTime = stageStartTime.getTime() 
                    } 
                container(name: 'kaniko') {
                    sh 'mkdir -p /kaniko/.docker'
                    sh "echo '{\\"auths\\":{\\"$docker_repo_host\\":{\\"username\\":\\"\${NEXUS_CRED_USR}\\",\\"password\\":\\"\${NEXUS_CRED_PSW}\\"}}}' >> /kaniko/.docker/config.json"
                    sh  '/kaniko/executor --custom-platform linux/arm64 --custom-platform linux/amd64 --dockerfile=\\"${dockerfile_path}\\" --cleanup --context "./" --destination=\\"$docker_repo_host\\"/\\"$APP_NAME\\":\\"\${VERSION}\\"'
                }
            }
            post {
                success {
                    script{
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Build Stage",
                            "job_status": "pass",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
                failure {
                    script{
                        def stageEndTime= new Date()
                        println stageEndTime.getTime()
                        env.stageTime = stageEndTime.getTime() - env.stageStartTime.toLong()
                    }
                    sh '''curl -X POST -H "Content-Type: application/json" ${build_stat_endpoint} -d '{
                            "job_name": "'"$APP_NAME"'",
                            "build_number": "'"\${VERSION}"'",
                            "job_stage": "Build Stage",
                            "job_status": "fail",
                            "stage_time": "'"\${stageTime}"'",
                            "tags": {
                                "tenant": "'"$tenant"'",
                                "vertical": "'"$vertical"'",
                                "pod": "'"$team"'"
                            }'}
                            '''
                }
            }
        }
        stage ('Scanning Phase') {
            parallel {
                stage('Scanning Init') {
                    steps {
                        sh 'echo starting scanning stage'
                    }
                }
                ${sonar_scan}
                ${trivy_scan}
                ${owasp_scan}
            }
        }
        ${dev_stage}
        ${stg_stage}
        ${prod_stage}
        ${central_stage}
        ${zap_scan}
    }
    post { 
        always { 
            script {
                currentBuild.displayName = "\${env.VERSION}-\${env.BUILD_NUMBER}"
                if ( currentBuild.description!= null && !currentBuild.description.isEmpty() ) {
                    currentBuild.description =  currentBuild.description + "<br><a href='\${env.BUILD_URL}restart'>Restart from Stage</a>"
                }
                else{
                    currentBuild.description =  "<a href='\${env.BUILD_URL}restart'>Restart from Stage</a>"
                }
                
            }
        }
    }
}
""")
                    sandbox()
                }
            }
        }
    }
}
