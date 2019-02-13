package com.github.slamdev.kubernetes.internal

import groovy.text.SimpleTemplateEngine
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream

import static java.nio.charset.StandardCharsets.UTF_8
import static java.nio.file.StandardOpenOption.CREATE_NEW

@CompileStatic
class K8sCopier {

    DeploySpec spec

    Path destination

    YamlMerger merger

    void copy() {
        List<Path> files = findNestedFiles(
                spec.inheritFromDir?.resolve('k8s'),
                spec.inputDir?.resolve('k8s')
        )
        spec.project.logger.info('found files for k8s: {}', files)
        files.findAll { isK8sFile(it) }
                .findAll { hasCorrectClassifiers(it, spec.classifiers) }
                .groupBy { resourceName(it) }
                .each { resource, fs -> copyFile(merge(fs, merger, spec), resource, destination, spec) }
    }

    private static String resourceName(Path file) {
        String name = file.fileName
        String[] parts = name.split('\\.')
        if (parts.length == 2 || parts.length == 3) {
            return parts[0]

        }
        throw new IllegalArgumentException("File ${file} does not correspond " +
                'to naming convention [resource].[optional-classifiers].yml')
    }

    private static String merge(List<Path> files, YamlMerger merger, DeploySpec spec) {
        spec.project.logger.info('merging k8s files: {}', files)
        List<String> yamls = files
                .collect { it }
                .sort { fileWeight(it, spec) }
                .collect { new String(Files.readAllBytes(it), UTF_8) }
        merger.mergeYamls(yamls as String[])
    }

    private static int fileWeight(Path file, DeploySpec spec) {
        int score = 0
        // if file is from project dir
        score += isParent(spec.inputDir, file) ? 10 : 0
        List<String> fileClassifiers = extractClassifiers(file)
        // if file has allowed classifiers
        score += fileClassifiers.isEmpty() || hasAllClassifier(fileClassifiers, spec.classifiers) ? 0 : -100
        // if file has max allowed classifiers
        score += countClassifiers(fileClassifiers, spec.classifiers)
        score
    }

    private static List<String> extractClassifiers(Path file) {
        String name = file.fileName
        String[] nameParts = name.split('\\.')
        if (nameParts.length == 2) {
            // file without classifier
            return []
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

    private static boolean hasCorrectClassifiers(Path file, List<String> allowedClassifiers) {
        List<String> classifiers = extractClassifiers(file)
        classifiers.empty || hasAllClassifier(classifiers, allowedClassifiers)
    }

    private static void copyFile(String content, String resource, Path destination, DeploySpec spec) {
        content = expand(content, spec.project.properties)
        Path file = destination.resolve("${resource}.yml")
        createDirectories(file)
        Files.write(file, content.getBytes(UTF_8), CREATE_NEW)
    }

    private static boolean isParent(Path parent, Path file) {
        parent != null && file.toString().contains(parent.toString())
    }

    private static String expand(String original, Map properties) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        engine.setEscapeBackslash(true)
        Writable result = engine.createTemplate(original).make(properties)
        result.toString()
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

    private static boolean isK8sFile(Path file) {
        String name = file.fileName
        name.endsWith('.yml') || name.endsWith('.yaml')
    }
}
