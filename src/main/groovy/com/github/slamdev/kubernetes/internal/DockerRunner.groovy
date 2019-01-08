package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic

import java.nio.file.Path

@CompileStatic
class DockerRunner {

    DeploySpec spec

    CommandLineExecutor executor

    Path directory

    void run() {
        String prefix = "${spec.project.path == ':' ? '' : spec.project.path}:deploy:docker:"
        def imageName = { String tag -> "${spec.dockerImageRepository}/${spec.dockerImageName}:${tag}" }
        List command = [
                'docker build --network=host',
                "-f ${directory.resolve('Dockerfile')}",
                spec.dockerImageTags.collect { "-t ${imageName(it)}" }.join(' '),
                "${spec.project.buildDir}"
        ]
        executor.exec(directory, prefix, command.join(' '))
        spec.dockerImageTags.each {
            String cmd = "docker push ${imageName(it)}"
            if (spec.dryRun) {
                spec.project.logger.lifecycle('Dry run for: {}', cmd)
            } else {
                executor.exec(directory, prefix, cmd)
            }
        }
    }
}
