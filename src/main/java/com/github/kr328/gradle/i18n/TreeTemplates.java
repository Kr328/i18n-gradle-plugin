package com.github.kr328.gradle.i18n;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public final class TreeTemplates {
    private final Child.Container root;

    private static Child.Container createContainerFor(Child.Container root, final FlattenTemplates.Key key) throws ProcessorException {
        for (final String name : key.getNames()) {
            final Child child = root.children.computeIfAbsent(name, (_k) -> new Child.Container(new LinkedHashMap<>()));
            if (!(child instanceof Child.Container)) {
                throw new ProcessorException("Except container but got value: " + key);
            }

            root = (Child.Container) child;
        }

        return root;
    }

    public static TreeTemplates createFrom(final FlattenTemplates flatten) throws ProcessorException {
        final Child.Container root = new Child.Container(new LinkedHashMap<>());

        for (final FlattenTemplates.Key key : flatten.getTemplates().keySet()) {
            final FlattenTemplates.Key containerKey = new FlattenTemplates.Key(key.getNames().subList(0, key.getNames().size() - 1));
            final Child.Container container = createContainerFor(root, containerKey);

            container.children.put(key.getNames().get(key.getNames().size() - 1), new Child.Value(key, flatten.getTemplates().get(key)));
        }

        return new TreeTemplates(root);
    }

    public interface Child {
        @Data
        final class Container implements Child {
            private final Map<String, Child> children;
        }

        @Data
        final class Value implements Child {
            private final FlattenTemplates.Key key;
            private final Template template;
        }
    }
}
