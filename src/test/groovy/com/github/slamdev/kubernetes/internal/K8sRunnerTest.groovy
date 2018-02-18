package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Test

import java.nio.file.Path

import static org.mockito.Mockito.*

@CompileStatic
class K8sRunnerTest extends GradleTest {

    String yamlWithNamespace = '''
|apiVersion: "v1"
|kind: "Pod"
|metadata:
|  name: "base"
|  namespace: "cool-namespace"
'''.stripMargin()

    String yamlWithoutNamespace = '''
|apiVersion: "v1"
|kind: "Pod"
|metadata:
|  name: "base"
'''.stripMargin()

    CommandLineExecutor executor

    @Before
    void before() {
        executor = mock(CommandLineExecutor)
    }

    @Test
    void should_run_k8s_commands_with_namespace() {
        Path directory = file('project/k8s')
        prepareFiles([
                'project/k8s/pod.yml': yamlWithNamespace
        ])
        new K8sRunner(spec: spec(), executor: executor, directory: directory).run()
        String buildCommand = "kubectl apply -f ${directory}"
        verify(executor, times(1)).exec(directory, ':deploy:k8s:', buildCommand)
        String rolloutStatusCommand = 'kubectl rollout status ' +
                "-f ${directory.resolve('pod.yml')} " +
                '--namespace=cool-namespace'
        verify(executor, times(1)).exec(directory, ':deploy:k8s:', rolloutStatusCommand, false)
        verifyNoMoreInteractions(executor)
    }

    @Test
    void should_run_k8s_commands_without_namespace() {
        Path directory = file('project/k8s')
        prepareFiles([
                'project/k8s/pod.yml': yamlWithoutNamespace
        ])
        new K8sRunner(spec: spec(), executor: executor, directory: directory).run()
        String buildCommand = "kubectl apply -f ${directory}"
        verify(executor, times(1)).exec(directory, ':deploy:k8s:', buildCommand)
        String rolloutStatusCommand = 'kubectl rollout status ' +
                "-f ${directory.resolve('pod.yml')}"
        verify(executor, times(1)).exec(directory, ':deploy:k8s:', rolloutStatusCommand, false)
        verifyNoMoreInteractions(executor)
    }
}
