package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

@CompileStatic
abstract class GradleTest {

    protected Project project

    @Before
    void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(Paths.get('build', Instant.now().toEpochMilli().toString()).toFile())
                .build()
        project.file('base/docker').mkdirs()
        project.file('project/docker').mkdirs()
        project.file('base/k8s').mkdirs()
        project.file('project/k8s').mkdirs()
    }

    protected DeploySpec spec() {
        new DeploySpec(
                project: project,
                outputDir: file('output'),
                classifiers: ['java', 'dev'],
                dockerImageName: 'sample',
                dockerImageRepository: 'repo.io',
                dockerImageTags: ['latest', 'prod'],
                inheritFromDir: file('base'),
                inputDir: file('project')
        )
    }

    protected Path file(String name) {
        project.file(name).toPath()
    }

    protected void prepareFiles(List<String> files) {
        prepareFiles(files.collectEntries {
            [(it): it]
        })
    }

    protected void prepareFiles(Map<String, String> files) {
        files.each { name, content ->
            Path file = file(name)
            Files.createDirectories(file.parent)
            file << content
        }
    }

    protected void ext(String name, String value) {
        ExtraPropertiesExtension ext = project.extensions.getByType(ExtraPropertiesExtension)
        ext.set(name, value)
    }
}
