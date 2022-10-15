package com.github.kr328.gradle.i18n;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public final class Parser {
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));
    private static final String ROOT_ELEMENT = "strings";

    private static void parseObjectInto(
            final Stack<String> context,
            final Map<FlattenTemplates.Key, Template> output,
            final Map<?, ?> object
    ) throws ProcessorException {
        for (final Object key : object.keySet()) {
            if (!(key instanceof String)) {
                throw new ProcessorException("Unexpected key: " + key);
            }

            final String keyText = (String) key;
            if (!keyText.matches("[a-z_]+")) {
                throw new ProcessorException("Unexpected key: " + key);
            }

            context.push(keyText);
            try {
                final Object value = object.get(keyText);
                if (value instanceof Map<?, ?>) {
                    parseObjectInto(context, output, (Map<?, ?>) value);
                } else if (value instanceof String || value instanceof Number) {
                    output.put(new FlattenTemplates.Key(new ArrayList<>(context)), Template.parse(value.toString()));
                } else {
                    throw new ProcessorException("Unsupported value: " + value);
                }
            } finally {
                context.pop();
            }
        }
    }

    public static FlattenTemplates parseFile(final Path path) throws IOException, ProcessorException {
        try (final InputStream input = Files.newInputStream(path)) {
            final Map<?, ?> yaml = YAML.load(input);

            if (yaml.size() != 1) {
                throw new ProcessorException("Expected 1 element in file " + path.toAbsolutePath() + " but got " + yaml.size());
            }

            if (!(yaml.get(ROOT_ELEMENT) instanceof Map<?, ?>)) {
                throw new ProcessorException("Root element " + ROOT_ELEMENT + " not found in " + path.toAbsolutePath());
            }

            final LinkedHashMap<FlattenTemplates.Key, Template> templates = new LinkedHashMap<>();

            parseObjectInto(new Stack<>(), templates, (Map<?, ?>) yaml.get(ROOT_ELEMENT));

            return new FlattenTemplates(templates);
        }
    }

    public static FlattenTemplates parseDirectory(final Path path) throws IOException, ProcessorException {
        try (final DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
            final FlattenTemplates result = new FlattenTemplates(new LinkedHashMap<>());

            for (final Path file : files) {
                final FlattenTemplates current = parseFile(file);
                for (final FlattenTemplates.Key key : current.getTemplates().keySet()) {
                    if (result.getTemplates().containsKey(key)) {
                        throw new ProcessorException("Duplicate key " + key);
                    }
                }

                result.getTemplates().putAll(current.getTemplates());
            }

            return result;
        }
    }
}
