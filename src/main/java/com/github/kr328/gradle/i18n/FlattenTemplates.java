package com.github.kr328.gradle.i18n;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public final class FlattenTemplates {
    private final Map<Key, Template> templates;

    @Data
    public static class Key {
        private final List<String> names;
    }
}
