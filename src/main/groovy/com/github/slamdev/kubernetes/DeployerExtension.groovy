package com.github.slamdev.kubernetes

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@CompileStatic
class DeployerExtension {

    Property<File> outputDir

    ListProperty<String> classifiers

    Property<String> dockerImageName

    Property<String> dockerImageRepository

    ListProperty<String> dockerImageTags

    Property<File> inheritFromDir

    Property<File> inputDir

    Property<Boolean> dryRun

    DeployerExtension(Project project) {
        outputDir = project.objects.property(File)
        setOutputDir(project.file("${project.buildDir}/deploy"))
        classifiers = project.objects.listProperty(String)
        dockerImageName = project.objects.property(String)
        setDockerImageName(project.name)
        dockerImageRepository = project.objects.property(String)
        dockerImageTags = project.objects.listProperty(String)
        setDockerImageTags(['latest'])
        inheritFromDir = project.objects.property(File)
        setInheritFromDir(exists(project.rootProject.file('ops/deploy')))
        inputDir = project.objects.property(File)
        setInputDir(exists(project.file('src/deploy')))
        dryRun = project.objects.property(Boolean)
        setDryRun(false)
    }

    void setOutputDir(File outputDir) {
        this.outputDir.set(outputDir)
    }

    void setClassifiers(List<String> classifiers) {
        this.classifiers.set(classifiers)
    }

    void setDockerImageName(String dockerImageName) {
        this.dockerImageName.set(dockerImageName)
    }

    void setDockerImageRepository(String dockerImageRepository) {
        this.dockerImageRepository.set(dockerImageRepository)
    }

    void setDockerImageTags(List<String> dockerImageTags) {
        this.dockerImageTags.set(dockerImageTags)
    }

    void setInheritFromDir(File inheritFromDir) {
        this.inheritFromDir.set(inheritFromDir)
    }

    void setInputDir(File inputDir) {
        this.inputDir.set(inputDir)
    }

    void setDryRun(boolean dryRun) {
        this.dryRun.set(dryRun)
    }

    private static File exists(File file) {
        file.exists() ? file : null
    }
}
