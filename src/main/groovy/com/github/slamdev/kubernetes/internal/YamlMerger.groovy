package com.github.slamdev.kubernetes.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import groovy.transform.CompileStatic

@CompileStatic
class YamlMerger {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())

    String mergeYamls(String... yamls) {
        if (yamls.length == 0) {
            throw new IllegalArgumentException('Yamls should not be empty')
        }
        if (yamls.length == 1) {
            return yamls[0]
        }
        List<String> copy = new ArrayList<>(yamls as List)
        JsonNode all = MAPPER.readTree(copy.remove(0) as String)
        copy.each { all = merge(all, MAPPER.readTree(it as String)) }
        asString(all)
    }

    private JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        Iterator<String> fieldNames = updateNode.fieldNames()
        while (fieldNames.hasNext()) {
            //noinspection ChangeToOperator
            String updatedFieldName = fieldNames.next()
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName)
            JsonNode updatedValue = updateNode.get(updatedFieldName)
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() && updatedValue.isArray()) {
                ArrayNode updatedArrayNode = (ArrayNode) updatedValue
                ArrayNode arrayNodeToBeUpdated = (ArrayNode) valueToBeUpdated
                for (int i = 0; updatedArrayNode.has(i); ++i) {
                    if (arrayNodeToBeUpdated.has(i)) {
                        JsonNode mergedNode = merge(arrayNodeToBeUpdated.get(i), updatedArrayNode.get(i))
                        arrayNodeToBeUpdated.set(i, mergedNode)
                    } else {
                        arrayNodeToBeUpdated.add(updatedArrayNode.get(i))
                    }
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null
                    && updatedValue != null
                    && valueToBeUpdated.isObject()
                    && !updatedValue.isNull()) {
                merge(valueToBeUpdated, updatedValue)
            } else {
                if (updatedValue == null || updatedValue.isNull()) {
                    ((ObjectNode) mainNode).remove(updatedFieldName)
                } else {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue)
                }
            }
        }
        (updateNode instanceof TextNode ?: mainNode) as JsonNode
    }

    private static String asString(JsonNode node) {
        StringWriter writer = new StringWriter()
        YAMLGenerator generator = new YAMLFactory().createGenerator(writer)
        MAPPER.writeTree(generator, node)
        writer.toString()
    }
}
