package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import org.junit.Ignore
import org.junit.Test

import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class DockerCopierTest extends GradleTest {

    @Test
    void should_prefer_project_files() {
        List<String> files = [
                'base/docker/Dockerfile',
                'base/docker/1.txt',
                'base/docker/2.txt',
                'project/docker/Dockerfile',
                'project/docker/2.txt'
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('project/docker/Dockerfile')
        assertThat(file('output/docker/1.txt')).hasContent('base/docker/1.txt')
        assertThat(file('output/docker/2.txt')).hasContent('project/docker/2.txt')
    }

    @Test
    void should_prefer_classifier_base_files() {
        List<String> files = [
                'base/docker/Dockerfile',
                'base/docker/Dockerfile.java',
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('base/docker/Dockerfile.java')
    }

    @Test
    void should_prefer_project_files_ignoring_base_classifiers() {
        List<String> files = [
                'base/docker/Dockerfile',
                'base/docker/Dockerfile.java-dev',
                'project/docker/Dockerfile',
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('project/docker/Dockerfile')
    }

    @Test
    void should_prefer_all_classifiers_project_files() {
        List<String> files = [
                'project/docker/Dockerfile',
                'project/docker/Dockerfile.java',
                'project/docker/Dockerfile.java-dev',
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('project/docker/Dockerfile.java-dev')
    }

    @SuppressWarnings('GStringExpressionWithinString')
    @Test
    @Ignore
    void should_expand_non_docker_base_files() {
        Map<String, String> files = [
                'base/docker/Dockerfile': '${test}',
                'base/docker/1.txt'     : '${test}',
        ]
        prepareFiles(files)
        ext('test', 'value')
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('${test}')
        assertThat(file('output/docker/1.txt')).hasContent('value')
    }

    @SuppressWarnings('GStringExpressionWithinString')
    @Test
    @Ignore
    void should_expand_non_docker_project_files() {
        Map<String, String> files = [
                'project/docker/Dockerfile': '${test}',
                'project/docker/1.txt'     : '${test}',
        ]
        prepareFiles(files)
        ext('test', 'value')
        new DockerCopier(spec: spec(), destination: dest()).copy()
        assertThat(file('output/docker/Dockerfile')).hasContent('${test}')
        assertThat(file('output/docker/1.txt')).hasContent('value')
    }

    @Test(expected = IllegalArgumentException)
    void should_error_on_badly_named_base_docker_file() {
        List<String> files = [
                'base/docker/Dockerfile.java.dev',
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
    }

    @Test(expected = IllegalArgumentException)
    void should_error_on_badly_named_project_docker_file() {
        List<String> files = [
                'project/docker/Dockerfile.java.dev',
        ]
        prepareFiles(files)
        new DockerCopier(spec: spec(), destination: dest()).copy()
    }

    private Path dest() {
        file('output/docker/')
    }
}
