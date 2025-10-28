@Library('jenkins-shared-lib') _

  def working_directory = 'eo-dagility-gitproxy'
  def veracode_sandbox_name = 'dagility-git-proxy'
  def module_name = 'dagility-git-proxy'

  properties([
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')),
    parameters([
      booleanParam(defaultValue: true, description: '', name: 'build'),
      booleanParam(defaultValue: true, description: '', name: 'deploy'),
      choice(choices: ['CI'], description: '', name: 'jobtype'),
      choice(choices: ['dev', 'demo'], description: '', name: 'environment'),
      string(defaultValue: 'latest', description: '', name: 'image_tag', trim: true),
      string(defaultValue: 'main', description: '', name: 'branch', trim: true),
      string(defaultValue: 'master', description: '', name: 'secondaryBranch', trim: true),
      booleanParam(defaultValue: false, description: '', name:'sca'),
      booleanParam(defaultValue: true, description: '', name: 'artifact')
    ]),
  ] + getGitlabTriggerSetup()
  )


  EdgeopsCIPipeline(branch: "${branch}", secondaryBranch: "${secondaryBranch}", deploy: "${deploy}", environment: "${environment}", image_tag: "${image_tag}", module_name: "${module_name}", working_directory: "${working_directory}", veracode_sandbox_name: "${veracode_sandbox_name}")
