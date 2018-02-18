package com.github.slamdev.kubernetes.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
class K8sRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())

    DeploySpec spec

    CommandLineExecutor executor

    Path directory

    void run() {
        String prefix = "${spec.project.path == ':' ? '' : spec.project.path}:deploy:k8s:"
        String command = "kubectl apply -f ${directory}"
        if (spec.dryRun) {
            command += ' --dry-run=true'
        }
        executor.exec(directory, prefix, command)
        Files
                .walk(directory)
                .filter { Path file -> isK8sFile(file) }
                .each { Path file -> checkRolloutStatus(file, prefix, executor, spec.dryRun, spec.project.logger) }
    }

    private static void checkRolloutStatus(Path file, String prefix, CommandLineExecutor executor,
                                           boolean dryRun, Logger logger) {
        String command = "kubectl rollout status -f ${file}"
        String namespace = extractNamespace(file)
        if (namespace != null) {
            command += " --namespace=${namespace}"
        }
        if (dryRun) {
            logger.lifecycle('Dry run for: {}', command)
        } else {
            executor.exec(file.parent, prefix, command, false)
        }
    }

    private static String extractNamespace(Path file) {
        JsonNode root = MAPPER.readTree(file.toFile())
        if (exists(root)) {
            JsonNode metadata = root.get('metadata')
            if (exists(metadata)) {
                JsonNode namespace = metadata.get('namespace')
                if (exists(namespace)) {
                    return namespace.textValue()
                }
            }
        }
        null
    }

    private static boolean exists(JsonNode node) {
        node != null && !node.isNull()
    }

    private static boolean isK8sFile(Path file) {
        String name = file.fileName
        name.endsWith('.yml') || name.endsWith('.yaml')
    }
}
