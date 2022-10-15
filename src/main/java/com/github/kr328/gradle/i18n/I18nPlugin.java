package com.github.kr328.gradle.i18n;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.tasks.ExtractDeepLinksTask;
import com.android.build.gradle.tasks.MergeResources;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet;
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget;
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget;
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

import javax.annotation.Nonnull;
import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public class I18nPlugin implements org.gradle.api.Plugin<Project> {
    @Override
    public void apply(@Nonnull final Project target) {
        if (!target.getPlugins().hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            throw new GradleException("Must apply kotlin multiplatform plugin");
        }

        final I18nExtension i18nExtension = target.getExtensions().create("i18n", I18nExtension.class);
        final Path generatedPath = Path.of(target.getBuildDir().getAbsolutePath(), "generated", "i18n");
        final Path generatedCommon = generatedPath.resolve("commonKotlin");
        final Path generatedAndroidResPath = generatedPath.resolve("androidResource");
        final Path generatedJvmResPath = generatedPath.resolve("jvmResource");
        final Path generatedAndroidKtPath = generatedPath.resolve("androidKotlin");
        final Path generatedJvmKtPath = generatedPath.resolve("jvmKotlin");

        final TaskContainer tasks = target.getTasks();
        final I18nTask i18nTask = tasks.create("generateI18nSources", I18nTask.class);
        i18nTask.getI18nDirectory().value(i18nExtension.getI18nDirectory());
        i18nTask.getPackageName().value(i18nExtension.getPackageName());
        i18nTask.getIsComposeEnabled().value(i18nExtension.getIsComposeEnabled());
        i18nTask.getLanguages().value(target.provider(i18nExtension::getLanguages));
        i18nTask.getCommonKotlinDirectory().set(generatedCommon.toAbsolutePath().toFile());
        i18nTask.getAndroidResourceDirectory().set(generatedAndroidResPath.toAbsolutePath().toFile());
        i18nTask.getAndroidKotlinDirectory().set(generatedAndroidKtPath.toAbsolutePath().toFile());
        i18nTask.getJvmResourceDirectory().set(generatedJvmResPath.toAbsolutePath().toFile());
        i18nTask.getJvmKotlinDirectory().set(generatedJvmKtPath.toAbsolutePath().toFile());

        tasks.withType(KotlinCompile.class, t -> t.dependsOn(i18nTask));
        tasks.withType(ProcessResources.class, t -> t.dependsOn(i18nTask));
        tasks.withType(MergeResources.class, t -> t.dependsOn(i18nTask));
        tasks.withType(ExtractDeepLinksTask.class, t -> t.dependsOn(i18nTask));

        final KotlinMultiplatformExtension kotlin = target.getExtensions().getByType(KotlinMultiplatformExtension.class);
        kotlin.getSourceSets().getByName("commonMain").getKotlin().srcDir(generatedCommon.toFile());
        kotlin.getTargets().all((final KotlinTarget kotlinTarget) -> {
            if (kotlinTarget instanceof KotlinAndroidTarget) {
                kotlin.getSourceSets().getByName(kotlinTarget.getName() + "Main")
                        .getKotlin().srcDir(generatedAndroidKtPath);

                final BaseExtension base = target.getExtensions().getByType(BaseExtension.class);
                base.getSourceSets().getByName("main").getRes().srcDir(generatedAndroidResPath.toFile());

                i18nTask.getIsAndroidEnabled().value(true);
            } else if (kotlinTarget instanceof KotlinJvmTarget) {
                final NamedDomainObjectContainer<KotlinSourceSet> sourceSets = kotlin.getSourceSets();
                final KotlinSourceSet main = sourceSets.getByName(kotlinTarget.getName() + "Main");
                main.getKotlin().srcDir(generatedJvmKtPath);
                main.getResources().srcDir(generatedJvmResPath);

                i18nTask.getIsJvmEnabled().value(true);
            }
        });
    }
}
