package com.github.slamdev.kubernetes.internal

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class YamlMergerTest {

    YamlMerger merger = new YamlMerger()

    String yamlBase = '''
|apiVersion: "v1"
|kind: "Namespace"
|metadata:
|  name: "base"
|  text: "value"
'''.stripMargin()

    String yamlProject = '''
|apiVersion: "v1"
|kind: "Namespace"
|metadata:
|  name: "project"
'''.stripMargin()

    String yamlExpected = '''
|---
|apiVersion: "v1"
|kind: "Namespace"
|metadata:
|  name: "project"
|  text: "value"
'''.stripMargin()

    @Test
    void should_merge_yamls() {
        String result = merger.mergeYamls(yamlBase, yamlProject)
        assertThat(result).isEqualToIgnoringNewLines(yamlExpected)
    }
}
