package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic

import java.nio.file.Path

@CompileStatic
class Deployer {

    DeploySpec spec

    void deploy() {
        spec.project.logger.info('spec for deploy: {}', spec)
        cleanDirectory(spec.outputDir.toFile())
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
        new K8sCopier(spec: spec, destination: outputDir, merger: new YamlMerger()).copy()
        new K8sRunner(spec: spec, directory: outputDir, executor: executor).run()
    }

    private static void cleanDirectory(File dir) {
        dir.listFiles().each { File file ->
            removeDirectory(file)
        }
    }

    private static void removeDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles()
            if (files != null && files.length > 0) {
                for (File aFile : files) {
                    removeDirectory(aFile)
                }
            }
            dir.delete()
        } else {
            dir.delete()
        }
    }
}
