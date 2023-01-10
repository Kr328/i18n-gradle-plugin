package com.github.kr328.gradle.i18n;

import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.ParameterizedTypeName;
import com.squareup.kotlinpoet.TypeName;

public final class Naming {
    public static final String I18N_IMPL_CLASS_NAME = "I18nImpl";
    public static final String I18N_CLASS_NAME = "I18n";
    public static final String I18N_COMPOSABLE_CLASS_NAME = "I18nComposable";

    public static final String JVM_RESOURCE_BUNDLE_NAME = "i18n";

    public static String implFunctionName(final FlattenTemplates.Key key) {
        return String.join("_", key.getNames());
    }

    public static String jvmResourceKey(final FlattenTemplates.Key key) {
        return "i18n." + String.join(".", key.getNames());
    }

    public static String androidResourceKey(final FlattenTemplates.Key key) {
        return "i18n_" + String.join("_", key.getNames());
    }

    public static TypeName formatterName(final String packageName, final TypeName returnType) {
        return ParameterizedTypeName.get(new ClassName(packageName, "Formatter"), returnType);
    }
}
