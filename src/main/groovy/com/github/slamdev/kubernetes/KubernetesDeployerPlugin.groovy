package com.github.slamdev.kubernetes

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class KubernetesDeployerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        DeployerExtension extension = project.extensions.create('deployer', DeployerExtension, project)
        project.tasks.create('deploy', DeployTask) { DeployTask task ->
            task.setOutputDir(extension.outputDir)
            task.setClassifiers(extension.classifiers)
            task.setDockerImageName(extension.dockerImageName)
            task.setDockerImageRepository(extension.dockerImageRepository)
            task.setDockerImageTags(extension.dockerImageTags)
            task.setInheritFromDir(extension.inheritFromDir)
            task.setInputDir(extension.inputDir)
        }
    }
}
