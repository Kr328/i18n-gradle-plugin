package com.github.kr328.gradle.i18n;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import javax.annotation.Nonnull;

public abstract class I18nExtension {
    @Nonnull
    public abstract DirectoryProperty getI18nDirectory();

    @Nonnull
    public abstract Property<String> getPackageName();

    @Nonnull
    public abstract Property<Boolean> getIsComposeEnabled();

    @Nonnull
    public abstract NamedDomainObjectContainer<Language> getLanguages();

    public void languages(@Nonnull Action<NamedDomainObjectContainer<Language>> action) {
        action.execute(getLanguages());
    }
}
