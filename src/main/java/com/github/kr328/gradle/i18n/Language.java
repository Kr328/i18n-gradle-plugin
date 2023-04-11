package com.github.kr328.gradle.i18n;

import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

@Data
public class Language implements Serializable {
    @Nonnull
    private final String name;
    @Nullable
    private String jvmLanguageTag;
    @Nullable
    private String androidLanguageTag;

    public Language(@Nonnull final String name) {
        this.name = name;

        if ("root".equals(name)) {
            throw new IllegalArgumentException("Invalid language name 'root'");
        }
    }
}
