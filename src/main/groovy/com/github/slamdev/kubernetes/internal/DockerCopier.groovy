package com.github.slamdev.kubernetes.internal

import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream

import static java.nio.charset.StandardCharsets.UTF_8

@CompileStatic
class DockerCopier {

    DeploySpec spec

    Path destination

    void copy() {
        List<Path> files = findNestedFiles(
                spec.inheritFromDir?.resolve('docker'),
                spec.inputDir?.resolve('docker')
        )
        spec.project.logger.info('found files for docker: {}', files)
        Path mainDockerFile = findMainDockerFile(files, spec)
        files
                .findAll { it == mainDockerFile || !isDockerFile(it) }
                .each { copyFile(it, destination, spec) }
    }

    private static void copyFile(Path file, Path destination, DeploySpec spec) {
        Path inheritFromDir = spec.inheritFromDir?.resolve('docker')
        Path inputDir = spec.inputDir?.resolve('docker')
        Path parent = isParent(inheritFromDir, file) ? inheritFromDir : inputDir
        Path base = parent.relativize(file)
        Path newFile = destination.resolve(base)
        createDirectories(newFile)
        if (newFile.fileName.toString().startsWith('Dockerfile')) {
            Files.copy(file, removeClassifiers(newFile))
        } else {
            // expand only non-Dockerfile since it is common to use env variables that conflicts with groovy $ sign
            String content = expand(file/*, spec.project.properties*/)
            Files.write(newFile, content.getBytes(UTF_8))
        }
    }

    private static Path removeClassifiers(Path file) {
        List<String> classifiers = extractDockerClassifiers(file)
        if (classifiers.isEmpty()) {
            return file
        }
        // remove classifiers
        file.parent.resolve('Dockerfile')
    }

    private static boolean isParent(Path parent, Path file) {
        parent != null && file.toString().contains(parent.toString())
    }

    private static String expand(Path file/*, Map properties*/) {
        new String(Files.readAllBytes(file), UTF_8)
        // temp disable property expanding for all docker related files
//        expand(new String(Files.readAllBytes(file), UTF_8), properties)
    }

    private static String expand(String original, Map properties) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        Writable result = engine.createTemplate(original).make(properties)
        result.toString()
    }

    private static List<String> extractDockerClassifiers(Path file) {
        String name = file.fileName
        String[] nameParts = name.split('\\.')
        if (nameParts.length == 1) {
            // file without classifier
            return []
        }
        if (nameParts.length > 2) {
            throw new IllegalArgumentException("File ${file} does not correspond " +
                    'to naming convention Dockerfile.[optional-classifiers]')
        }
        nameParts[1].split('-') as List<String>
    }

    private static boolean hasAllClassifier(List<String> classifiers, List<String> allowedClassifiers) {
        countClassifiers(classifiers, allowedClassifiers) == classifiers.size()
    }

    private static int countClassifiers(List<String> classifiers, List<String> allowedClassifiers) {
        int count = classifiers.size()
        List<String> absenceClassifiers = new ArrayList<>(classifiers)
        absenceClassifiers.removeAll(allowedClassifiers)
        count - absenceClassifiers.size()
    }

    private static createDirectories(Path file) {
        if (!Files.isDirectory(file)) {
            file = file.parent
        }
        if (!Files.exists(file)) {
            Files.createDirectories(file)
        }
    }

    @CompileDynamic(/* java 8 streams are not supported by groovy static compilation */)
    private static List<Path> findNestedFiles(Path... dirs) {
        Stream<Path> stream = Stream.empty()
        dirs.findAll { it != null && Files.exists(it) }.each { dir -> stream = Stream.concat(stream, Files.walk(dir)) }
        stream.filter { Path file -> !Files.isDirectory(file) }
                .collect(Collectors.<Path> toList())
    }

    private static boolean isDockerFile(Path file) {
        file.fileName.toString().startsWith('Docker')
    }

    private static Path findMainDockerFile(List<Path> files, DeploySpec spec) {
        files.findAll { isDockerFile(it) }.sort { Path file ->
            int score = 0
            // if file is from project dir
            score += isParent(spec.inputDir, file) ? 10 : 0
            List<String> fileClassifiers = extractDockerClassifiers(file)
            // if file has allowed classifiers
            score += fileClassifiers.isEmpty() || hasAllClassifier(fileClassifiers, spec.classifiers) ? 0 : -100
            // if file has max allowed classifiers
            score += countClassifiers(fileClassifiers, spec.classifiers)
            score
        }.last()
    }
}
