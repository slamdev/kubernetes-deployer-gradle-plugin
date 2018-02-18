package com.github.slamdev.kubernetes.internal

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.Project

import java.nio.file.Path

@CompileStatic
@ToString(includeNames = true)
class DeploySpec {

    Project project

    Path outputDir

    List<String> classifiers

    String dockerImageName

    String dockerImageRepository

    List<String> dockerImageTags

    Path inheritFromDir

    Path inputDir

    boolean dryRun
}
