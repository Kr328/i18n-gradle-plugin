# I18n for Kotlin Multiplatform

A gradle plugin to generate cross-platform i18n resources for Kotlin Multiplatform.

# Getting Start

### Add repository

settings.gradle.kts
```kotlin

pluginManagement {
    repositories {
        maven("https://maven.kr328.app/releases")
        // ...
    }
}
```

### Apply plugin

build.gradle.kts
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.github.kr328.gradle.i18n") version "1.0.0"
    // ...
}
```

### Configure plugin

build.gradle.kts
```kotlin
i18n {
    // Source directory
    i18nDirectory.set(file("i18n"))

    // Generated package name
    packageName.set(android.namespace!!)

    // Enable multiplatform compose support
    isComposeEnabled.set(true)

    // Languages
    languages {
        create("language_name") {
            jvmLanguageTag = "zh_CN"       // Jvm platform ResourceBundle tag
            androidLanguageTag = "zh-rCN"  // Android values/strings tag
        }
        // Other languages
    }
}
```

### Write i18n source

root/strings.yaml
```yaml
strings: # root element
  i18n_text: "Text Value" # plain text
  container: # sub element
    i18n_with_format_text: "Text With format text: int = {i: %d} string = {s: %s} float = {f: %.2f}"
```

_language_name_/strings.yaml
```yaml
strings: # root element
  i18n_text: "文本值" # plain text
  container: # sub element
    i18n_with_format_text: "可格式化的文本: int = {i: %d} string = {s: %s} float = {f: %.2f}"
```

### Using generated code

```kotlin
val i18n = createI18n(...) 

println(i18n.i18n_text())
println(i18n.container.i18n_with_format_text(1L, "114514", 0.2))
```