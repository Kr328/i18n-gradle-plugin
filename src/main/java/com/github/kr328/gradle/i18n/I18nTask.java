package com.github.kr328.gradle.i18n;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.nio.file.Path;

public abstract class I18nTask extends DefaultTask {
    @Input
    public abstract Property<Boolean> getIsJvmEnabled();

    @Input
    public abstract Property<Boolean> getIsAndroidEnabled();

    @Input
    public abstract Property<Boolean> getIsComposeEnabled();

    @InputDirectory
    public abstract DirectoryProperty getI18nDirectory();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract SetProperty<Language> getLanguages();

    @OutputDirectory
    public abstract DirectoryProperty getCommonKotlinDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getAndroidResourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getAndroidKotlinDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getJvmResourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getJvmKotlinDirectory();

    @TaskAction
    public void doAction() throws Exception {
        final boolean isJvmEnabled = getIsJvmEnabled().getOrElse(false);
        final boolean isAndroidEnabled = getIsAndroidEnabled().getOrElse(false);
        final boolean isComposeEnabled = getIsComposeEnabled().getOrElse(false);

        final Path commonKtPath = getCommonKotlinDirectory().get().getAsFile().toPath();
        final Path androidResPath = getAndroidResourceDirectory().get().getAsFile().toPath();
        final Path androidKtPath = getAndroidKotlinDirectory().get().getAsFile().toPath();
        final Path jvmResPath = getJvmResourceDirectory().get().getAsFile().toPath();
        final Path jvmKtPath = getJvmKotlinDirectory().get().getAsFile().toPath();

        FilesUtils.deleteChildren(commonKtPath);
        FilesUtils.deleteChildren(androidResPath);
        FilesUtils.deleteChildren(androidKtPath);
        FilesUtils.deleteChildren(jvmResPath);
        FilesUtils.deleteChildren(jvmKtPath);

        final Path i18nPath = getI18nDirectory().getAsFile().get().toPath();
        final FlattenTemplates root = Parser.parseDirectory(i18nPath.resolve("root"));
        final Generator generator = new Generator(root, getPackageName().get());

        generator.generateCommonKotlin(commonKtPath, false);
        if (isComposeEnabled) {
            generator.generateCommonKotlin(commonKtPath, true);
        }

        generator.generateCommonExpectKotlin(commonKtPath);

        if (isJvmEnabled) {
            generator.generateJvmKotlin(jvmKtPath);
            generator.generateJvmResource(jvmResPath, null, root);
        }

        if (isAndroidEnabled) {
            generator.generateAndroidKotlin(androidKtPath);
            generator.generateAndroidResource(androidResPath, null, root);
        }

        for (final Language language : getLanguages().get()) {
            final FlattenTemplates current = Parser.parseDirectory(i18nPath.resolve(language.getName()));
            if (isJvmEnabled) {
                final String jvmLanguageTag = language.getJvmLanguageTag();
                if (jvmLanguageTag == null) {
                    throw new ProcessorException("Jvm enabled but language " + language.getName() + " tag is not set");
                }

                generator.generateJvmResource(jvmResPath, jvmLanguageTag, current);
            }

            if (isAndroidEnabled) {
                final String androidLanguageTag = language.getAndroidLanguageTag();
                if (androidLanguageTag == null) {
                    throw new ProcessorException("Android enabled but language " + language.getName() + " tag is not set");
                }

                generator.generateAndroidResource(androidResPath, androidLanguageTag, current);
            }
        }
    }
}
