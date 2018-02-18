package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic

import java.nio.file.Path

@CompileStatic
class Deployer {

    DeploySpec spec

    void deploy() {
        CommandLineExecutor executor = new CommandLineExecutor(project: spec.project)
        deployDocker(executor, spec)
        deployK8s(executor, spec)
    }

    private static deployDocker(CommandLineExecutor executor, DeploySpec spec) {
        Path outputDir = spec.outputDir.resolve('docker')
        new DockerCopier(spec: spec, destination: outputDir).copy()
        new DockerRunner(spec: spec, directory: outputDir, executor: executor).run()
    }

    private static deployK8s(CommandLineExecutor executor, DeploySpec spec) {
        Path outputDir = spec.outputDir.resolve('k8s')
        new K8sCopier(spec: spec, destination: outputDir).copy()
        new K8sRunner(spec: spec, directory: outputDir, executor: executor).run()
    }
}
