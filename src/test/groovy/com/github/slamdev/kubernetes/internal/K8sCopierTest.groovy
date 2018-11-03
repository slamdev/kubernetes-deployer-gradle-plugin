package com.github.slamdev.kubernetes.internal

import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.when

class K8sCopierTest extends GradleTest {

    YamlMerger merger

    @Before
    void before() {
        merger = Mockito.mock(YamlMerger)
        when(merger.mergeYamls(Mockito.<String[]> any())).thenAnswer { InvocationOnMock invocation ->
            invocation.arguments.join(' ')
        }
    }

    @Test
    @Ignore
    void should_merge() {
        List<String> files = [
                'base/k8s/pod.yml',
                'base/k8s/pod.java.yml',
                'base/k8s/pod.dev.yml',
                'project/k8s/pod.yml',
                'project/k8s/pod.dev.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(Files.walk(file('output/k8s'))).size().isEqualTo(2) // counts dir + nested files
        assertThat(file('output/k8s/pod.yml')).hasContent(files.join(' '))
    }

    @Test
    void should_merge_files() {
        List<String> files = [
                'base/k8s/ns.yml',
                'base/k8s/pod.yml',
                'project/k8s/pod.yml',
                'project/k8s/secret.yml'
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/ns.yml')).hasContent('base/k8s/ns.yml')
        assertThat(file('output/k8s/pod.yml')).hasContent('base/k8s/pod.yml project/k8s/pod.yml')
        assertThat(file('output/k8s/secret.yml')).hasContent('project/k8s/secret.yml')
    }

    @Test
    void should_merge_classifier_base_files() {
        List<String> files = [
                'base/k8s/pod.yml',
                'base/k8s/pod.java.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/pod.yml')).hasContent('base/k8s/pod.yml base/k8s/pod.java.yml')
    }

    @Test
    void should_merger_project_with_base_files_respecting_classifiers() {
        List<String> files = [
                'base/k8s/pod.yml',
                'base/k8s/pod.java-dev.yml',
                'project/k8s/pod.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/pod.yml'))
                .hasContent('base/k8s/pod.yml base/k8s/pod.java-dev.yml project/k8s/pod.yml')
    }

    @Test
    void should_merge_classifiers_respecting_order() {
        List<String> files = [
                'project/k8s/pod.yml',
                'project/k8s/pod.java.yml',
                'project/k8s/pod.java-dev.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/pod.yml'))
                .hasContent('project/k8s/pod.yml project/k8s/pod.java.yml project/k8s/pod.java-dev.yml')
    }

    @Test
    void should_ignore_unset_classifiers() {
        List<String> files = [
                'project/k8s/pod.yml',
                'project/k8s/pod.prod.yml',
                'project/k8s/pod.java-prod.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/pod.yml')).hasContent('project/k8s/pod.yml')
    }

    @SuppressWarnings('GStringExpressionWithinString')
    @Test
    void should_expand_base_files() {
        Map<String, String> files = [
                'base/k8s/nc.yml'      : '${test}',
                'base/k8s/pod.java.yml': '${test}',
        ]
        prepareFiles(files)
        ext('test', 'value')
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/nc.yml')).hasContent('value')
        assertThat(file('output/k8s/pod.yml')).hasContent('value')
    }

    @SuppressWarnings('GStringExpressionWithinString')
    @Test
    void should_expand_project_files() {
        Map<String, String> files = [
                'project/k8s/nc.yml'      : '${test}',
                'project/k8s/pod.java.yml': '${test}',
        ]
        prepareFiles(files)
        ext('test', 'value')
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
        assertThat(file('output/k8s/nc.yml')).hasContent('value')
        assertThat(file('output/k8s/pod.yml')).hasContent('value')
    }

    @Test(expected = IllegalArgumentException)
    void should_error_on_badly_named_base_docker_file() {
        List<String> files = [
                'base/k8s/nc.java.dev.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
    }

    @Test(expected = IllegalArgumentException)
    void should_error_on_badly_named_project_docker_file() {
        List<String> files = [
                'project/k8s/nc.java.dev.yml',
        ]
        prepareFiles(files)
        new K8sCopier(merger: merger, spec: spec(), destination: dest()).copy()
    }

    private Path dest() {
        file('output/k8s/')
    }
}
