package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Test

import java.nio.file.Path

import static org.mockito.Mockito.*

@CompileStatic
class DockerRunnerTest extends GradleTest {

    CommandLineExecutor executor

    @Before
    void before() {
        executor = mock(CommandLineExecutor)
    }

    @Test
    void should_run_docker_commands() {
        Path directory = file('project/docker')
        new DockerRunner(spec: spec(), executor: executor, directory: directory).run()
        String buildCommand = 'docker build -q ' +
                "-f ${directory.resolve('Dockerfile')} " +
                '-t repo.io/sample:latest ' +
                '-t repo.io/sample:prod ' +
                "${project.buildDir}"
        verify(executor, times(1)).exec(directory, ':deploy:docker:', buildCommand)
        String pushCommand1 = 'docker push repo.io/sample:latest'
        verify(executor, times(1)).exec(directory, ':deploy:docker:', pushCommand1)
        String pushCommand2 = 'docker push repo.io/sample:prod'
        verify(executor, times(1)).exec(directory, ':deploy:docker:', pushCommand2)
        verifyNoMoreInteractions(executor)
    }
}
