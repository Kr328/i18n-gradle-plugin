package com.github.kr328.gradle.i18n;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.ClassNames;
import com.squareup.kotlinpoet.MemberName;

import java.util.Locale;
import java.util.ResourceBundle;

public final class Symbols {
    public static final ClassName COMPOSABLE = new ClassName("androidx.compose.runtime", "Composable");
    public static final ClassName SUPPRESS = new ClassName("kotlin", "Suppress");
    public static final ClassName JVM_INLINE = new ClassName("kotlin.jvm", "JvmInline");
    public static final ClassName RESOURCES = new ClassName("android.content.res", "Resources");
    public static final ClassName RESOURCE_BUNDLE = ClassNames.get(ResourceBundle.class);
    public static final ClassName LOCALE = ClassNames.get(Locale.class);
    public static final MemberName REMEMBER = new MemberName("androidx.compose.runtime", "remember", true);
}
