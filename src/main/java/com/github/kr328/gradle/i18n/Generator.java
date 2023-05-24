package com.github.kr328.gradle.i18n;

import com.squareup.kotlinpoet.*;
import lombok.AllArgsConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@AllArgsConstructor
public class Generator {
    private static final AnnotationSpec KOTLIN_FILE_ANNOTATION = AnnotationSpec.builder(Symbols.SUPPRESS)
            .addMember("%S, %S, %S, %S, %S", "RedundantVisibilityModifier", "FunctionName", "PropertyName", "RemoveExplicitTypeArguments", "MemberVisibilityCanBePrivate")
            .build();
    private final FlattenTemplates root;
    private final String packageName;

    private List<ParameterSpec> buildVariableParameters(final FlattenTemplates.Key key) {
        final Template template = Objects.requireNonNull(root.getTemplates().get(key));

        return template.getVariables().stream()
                .map(v -> ParameterSpec.builder(v.getName(), v.getType().kotlinType()).build())
                .collect(Collectors.toList());
    }

    private CodeBlock buildFormatParameters(final FlattenTemplates.Key key) {
        final Template template = Objects.requireNonNull(root.getTemplates().get(key));
        final List<String> variables = template.getVariables()
                .stream().map(Template.Part.Variable::getName)
                .collect(Collectors.toList());
        final String format = String.join(",", Collections.nCopies(variables.size(), "%N"));

        return CodeBlock.of(format, variables.toArray(Object[]::new));
    }

    public void generateCommonExpectKotlin(final Path path) throws IOException {
        final TypeSpec.Builder implClass = TypeSpec.expectClassBuilder(Naming.I18N_IMPL_CLASS_NAME);

        for (final FlattenTemplates.Key key : root.getTemplates().keySet()) {
            if (buildFormatParameters(key).isNotEmpty()) {
                final TypeVariableName returnType = TypeVariableName.get("T");

                implClass.addFunction(
                        FunSpec.builder(Naming.implFunctionName(key))
                                .addTypeVariable(returnType)
                                .addParameter("F", Naming.formatterName(packageName, returnType))
                                .addParameters(buildVariableParameters(key))
                                .returns(returnType)
                                .build()
                );
            } else {
                implClass.addFunction(
                        FunSpec.builder(Naming.implFunctionName(key))
                                .returns(TypeNames.STRING)
                                .build()
                );
            }
        }

        FileSpec.builder(packageName, Naming.I18N_IMPL_CLASS_NAME)
                .addAnnotation(KOTLIN_FILE_ANNOTATION)
                .addType(implClass.build())
                .build()
                .writeTo(path);
    }

    public void generateCommonKotlin(final Path path, final boolean composable) throws ProcessorException, IOException {
        final ClassName implClassName = new ClassName(packageName, Naming.I18N_IMPL_CLASS_NAME);

        final BiFunction<ClassName, TreeTemplates.Child.Container, TypeSpec> generateType = new BiFunction<>() {
            @Override
            public TypeSpec apply(final ClassName className, final TreeTemplates.Child.Container root) {
                final TypeSpec.Builder rootType = TypeSpec.valueClassBuilder(className.getSimpleName())
                        .addAnnotation(Symbols.JVM_INLINE)
                        .addProperty(
                                PropertySpec.builder("IMPL", implClassName)
                                        .initializer("IMPL")
                                        .build()
                        )
                        .primaryConstructor(
                                FunSpec.constructorBuilder()
                                        .addParameter("IMPL", implClassName)
                                        .build()
                        );

                for (final Map.Entry<String, TreeTemplates.Child> entry : root.getChildren().entrySet()) {
                    if (entry.getValue() instanceof TreeTemplates.Child.Value) {
                        final TreeTemplates.Child.Value value = (TreeTemplates.Child.Value) entry.getValue();

                        final CodeBlock formatParameters = buildFormatParameters(value.getKey());
                        if (formatParameters.isNotEmpty()) {
                            final FunSpec.Builder withFormatter = FunSpec.builder(entry.getKey());
                            final FunSpec.Builder withoutFormatter = FunSpec.builder(entry.getKey());

                            final TypeVariableName returnType = TypeVariableName.get("T");
                            final List<ParameterSpec> parameters = buildVariableParameters(value.getKey());

                            withFormatter.addTypeVariable(returnType)
                                    .addParameter("F", Naming.formatterName(packageName, returnType))
                                    .addParameters(parameters)
                                    .returns(returnType);
                            withoutFormatter
                                    .addParameters(parameters)
                                    .returns(TypeNames.STRING)
                                    .addCode("return %N(DefaultFormatter, %L)", entry.getKey(), formatParameters);

                            if (composable) {
                                withFormatter.addAnnotation(Symbols.COMPOSABLE);
                                withoutFormatter.addAnnotation(Symbols.COMPOSABLE);

                                withFormatter.addCode(
                                        "return %M(IMPL, F, %L) { IMPL.%N(F, %L) }",
                                        Symbols.REMEMBER,
                                        formatParameters,
                                        Naming.implFunctionName(value.getKey()),
                                        formatParameters
                                );
                            } else {
                                withFormatter.addCode(
                                        "return IMPL.%N(F, %L)",
                                        Naming.implFunctionName(value.getKey()),
                                        formatParameters
                                );
                            }

                            rootType.addFunction(withFormatter.build());
                            rootType.addFunction(withoutFormatter.build());
                        } else {
                            final FunSpec.Builder fun = FunSpec.builder(entry.getKey())
                                    .returns(TypeNames.STRING);

                            if (composable) {
                                fun.addAnnotation(Symbols.COMPOSABLE);

                                fun.addCode(
                                        "return %M(IMPL) { IMPL.%N() }",
                                        Symbols.REMEMBER,
                                        Naming.implFunctionName(value.getKey())
                                );
                            } else {
                                fun.addCode(
                                        "return IMPL.%N()",
                                        Naming.implFunctionName(value.getKey())
                                );
                            }

                            rootType.addFunction(fun.build());
                        }
                    } else if (entry.getValue() instanceof TreeTemplates.Child.Container) {
                        final TreeTemplates.Child.Container container = (TreeTemplates.Child.Container) entry.getValue();
                        final ClassName childClassName = className.nestedClass(NameUtils.snakeToCamel(entry.getKey()));
                        final TypeSpec childType = apply(childClassName, container);

                        rootType.addType(childType).addProperty(
                                PropertySpec.builder(entry.getKey(), childClassName)
                                        .getter(FunSpec.getterBuilder().addCode("return %T(IMPL)", childClassName).build())
                                        .build()
                        );
                    }
                }

                return rootType.build();
            }
        };

        final String className;
        if (composable) {
            className = Naming.I18N_COMPOSABLE_CLASS_NAME;
        } else {
            className = Naming.I18N_CLASS_NAME;
        }

        FileSpec.builder(packageName, className)
                .addAnnotation(KOTLIN_FILE_ANNOTATION)
                .addType(generateType.apply(new ClassName(packageName, className), TreeTemplates.createFrom(root).getRoot()))
                .build()
                .writeTo(path);
    }

    public void generateJvmKotlin(final Path path) throws IOException {
        final TypeSpec.Builder implClass = TypeSpec.classBuilder(Naming.I18N_IMPL_CLASS_NAME)
                .addModifiers(KModifier.ACTUAL)
                .addProperty(
                        PropertySpec.builder("RES", Symbols.RESOURCE_BUNDLE)
                                .initializer("RES")
                                .build()
                )
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("RES", Symbols.RESOURCE_BUNDLE)
                                .build()
                );

        for (final FlattenTemplates.Key key : root.getTemplates().keySet()) {
            final CodeBlock formatParameters = buildFormatParameters(key);

            final FunSpec.Builder fun = FunSpec.builder(Naming.implFunctionName(key))
                    .addModifiers(KModifier.ACTUAL);

            if (formatParameters.isNotEmpty()) {
                final TypeVariableName returnType = TypeVariableName.get("T");

                fun.addTypeVariable(returnType)
                        .addParameter("F", Naming.formatterName(packageName, returnType))
                        .addParameters(buildVariableParameters(key))
                        .returns(returnType)
                        .addCode(
                                "return F.format(RES.locale, RES.getString(%S), %L)",
                                Naming.jvmResourceKey(key),
                                formatParameters
                        );
            } else {
                fun.returns(TypeNames.STRING)
                        .addCode("return RES.getString(%S)", Naming.jvmResourceKey(key));
            }

            implClass.addFunction(fun.build());
        }

        final ClassName i18nClassName = new ClassName(packageName, Naming.I18N_CLASS_NAME);
        final ClassName i18nComposableName = new ClassName(packageName, Naming.I18N_COMPOSABLE_CLASS_NAME);
        final ClassName i18nImplClassName = new ClassName(packageName, Naming.I18N_IMPL_CLASS_NAME);
        final FunSpec createI18nFunc = FunSpec.builder("createI18n")
                .addParameter(
                        ParameterSpec.builder("locale", Symbols.LOCALE)
                                .defaultValue("%T.getDefault()", Symbols.LOCALE)
                                .build()
                )
                .returns(i18nClassName)
                .addCode(
                        "return %T(%T(%T.getBundle(%S, locale)))",
                        i18nClassName,
                        i18nImplClassName,
                        Symbols.RESOURCE_BUNDLE,
                        String.join(".", packageName, Naming.JVM_RESOURCE_BUNDLE_NAME)
                )
                .build();
        final FunSpec createI18nComposableFunc = FunSpec.builder("createI18nComposable")
                .addParameter(
                        ParameterSpec.builder("locale", Symbols.LOCALE)
                                .defaultValue("%T.getDefault()", Symbols.LOCALE)
                                .build()
                )
                .returns(i18nComposableName)
                .addCode(
                        "return %T(%T(%T.getBundle(%S, locale)))",
                        i18nComposableName,
                        i18nImplClassName,
                        Symbols.RESOURCE_BUNDLE,
                        String.join(".", packageName, Naming.JVM_RESOURCE_BUNDLE_NAME)
                )
                .build();

        FileSpec.builder(packageName, Naming.I18N_IMPL_CLASS_NAME)
                .addAnnotation(KOTLIN_FILE_ANNOTATION)
                .addType(implClass.build())
                .addFunction(createI18nFunc)
                .addFunction(createI18nComposableFunc)
                .build()
                .writeTo(path);
    }

    public void generateAndroidKotlin(final Path path) throws IOException {
        final TypeSpec.Builder implClass = TypeSpec.classBuilder(Naming.I18N_IMPL_CLASS_NAME)
                .addModifiers(KModifier.ACTUAL)
                .addProperty(
                        PropertySpec.builder("RES", Symbols.RESOURCES)
                                .initializer("RES")
                                .build()
                )
                .primaryConstructor(
                        FunSpec.constructorBuilder()
                                .addParameter("RES", Symbols.RESOURCES)
                                .build()
                );

        for (final FlattenTemplates.Key key : root.getTemplates().keySet()) {
            final CodeBlock formatParameters = buildFormatParameters(key);

            final FunSpec.Builder fun = FunSpec.builder(Naming.implFunctionName(key))
                    .addModifiers(KModifier.ACTUAL);

            if (formatParameters.isNotEmpty()) {
                final TypeVariableName returnType = TypeVariableName.get("T");

                fun.addTypeVariable(returnType)
                        .addParameter("F", Naming.formatterName(packageName, returnType))
                        .returns(returnType)
                        .addParameters(buildVariableParameters(key))
                        .addCode(
                                "return F.format(RES.configuration.locales[0], RES.getString(R.string.%N), %L)",
                                Naming.androidResourceKey(key),
                                formatParameters
                        );
            } else {
                fun.returns(TypeNames.STRING)
                        .addCode("return RES.getString(R.string.%N)", Naming.androidResourceKey(key));
            }

            implClass.addFunction(fun.build());
        }

        final ClassName i18nClassName = new ClassName(packageName, Naming.I18N_CLASS_NAME);
        final ClassName i18nComposableName = new ClassName(packageName, Naming.I18N_COMPOSABLE_CLASS_NAME);
        final ClassName i18nImplClassName = new ClassName(packageName, Naming.I18N_IMPL_CLASS_NAME);
        final FunSpec createI18nFunc = FunSpec.builder("createI18n")
                .addParameter("resources", Symbols.RESOURCES)
                .returns(i18nClassName)
                .addCode(
                        "return %T(%T(resources))",
                        i18nClassName,
                        i18nImplClassName
                )
                .build();
        final FunSpec createI18nComposableFunc = FunSpec.builder("createI18nComposable")
                .addParameter("resources", Symbols.RESOURCES)
                .returns(i18nComposableName)
                .addCode(
                        "return %T(%T(resources))",
                        i18nComposableName,
                        i18nImplClassName
                )
                .build();

        FileSpec.builder(packageName, Naming.I18N_IMPL_CLASS_NAME)
                .addAnnotation(KOTLIN_FILE_ANNOTATION)
                .addType(implClass.build())
                .addFunction(createI18nFunc)
                .addFunction(createI18nComposableFunc)
                .build()
                .writeTo(path);
    }

    private String createFormatTextFromTemplate(final FlattenTemplates.Key key, final Template template) throws ProcessorException {
        final StringBuilder builder = new StringBuilder();
        for (final Template.Part part : template.getParts()) {
            if (part instanceof Template.Part.Literal) {
                builder.append(((Template.Part.Literal) part).getText());
            } else if (part instanceof Template.Part.Variable) {
                final Template.Part.Variable variable = (Template.Part.Variable) part;
                final int index = root.getTemplates().get(key).getVariables().indexOf(variable);
                if (index < 0) {
                    throw new ProcessorException("Variable " + variable + " not found in root " + key);
                }

                builder.append("%").append(index + 1).append("$").append(variable.getFormat().substring(1));
            }
        }
        return builder.toString();
    }

    public void generateJvmResource(final Path path, final String languageTag, final FlattenTemplates languageTemplates) throws ProcessorException, IOException {
        final Properties properties = new Properties();

        for (final FlattenTemplates.Key key : root.getTemplates().keySet()) {
            final Template template = Optional.ofNullable(languageTemplates.getTemplates().get(key))
                    .orElseGet(() -> root.getTemplates().get(key));

            properties.setProperty(Naming.jvmResourceKey(key), createFormatTextFromTemplate(key, template));
        }

        final Path propertiesPath;
        if (languageTag == null) {
            propertiesPath = path.resolve(packageName.replace('.', '/')).resolve(Naming.JVM_RESOURCE_BUNDLE_NAME + ".properties");
        } else {
            propertiesPath = path.resolve(packageName.replace('.', '/')).resolve(Naming.JVM_RESOURCE_BUNDLE_NAME + "_" + languageTag + ".properties");
        }

        Files.createDirectories(propertiesPath.getParent());

        try (final OutputStream stream = Files.newOutputStream(propertiesPath)) {
            properties.store(stream, null);
        }
    }

    public void generateAndroidResource(final Path path, final String languageTag, final FlattenTemplates languageTemplates) throws ProcessorException, ParserConfigurationException, TransformerException, IOException {
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element resources = document.createElement("resources");

        for (final FlattenTemplates.Key key : root.getTemplates().keySet()) {
            final Template template = Optional.ofNullable(languageTemplates.getTemplates().get(key))
                    .orElseGet(() -> root.getTemplates().get(key));

            final Element string = document.createElement("string");
            string.setTextContent(createFormatTextFromTemplate(key, template).replace("\n", "\\n"));
            string.setAttribute("name", Naming.androidResourceKey(key));
            resources.appendChild(string);
        }

        document.appendChild(resources);

        final Path xmlPath;
        if (languageTag == null) {
            xmlPath = path.resolve("values").resolve("strings.xml");
        } else {
            xmlPath = path.resolve("values-" + languageTag).resolve("strings.xml");
        }

        Files.createDirectories(xmlPath.getParent());

        try (final OutputStream stream = Files.newOutputStream(xmlPath)) {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(stream));
        }
    }

    public void generateDefaultFormatter(final Path path) throws IOException {
        final TypeVariableName returnType = TypeVariableName.get("T");

        final TypeSpec formatter = TypeSpec.funInterfaceBuilder("Formatter")
                .addTypeVariable(returnType)
                .addFunction(
                        FunSpec.builder("format")
                                .addModifiers(KModifier.ABSTRACT)
                                .addParameter("locale", Symbols.LOCALE)
                                .addParameter("format", TypeNames.STRING)
                                .addParameter(
                                        ParameterSpec.builder("args", TypeNames.ANY.copy(true, List.of()))
                                                .addModifiers(KModifier.VARARG)
                                                .build()
                                )
                                .returns(returnType)
                                .build()
                ).build();

        final PropertySpec defaultFormatter = PropertySpec.builder(
                "DefaultFormatter",
                Naming.formatterName(packageName, TypeNames.STRING)
        ).initializer(
                "%T { locale, format, args -> String.format(locale, format, *args) }",
                Naming.formatterName(packageName, TypeNames.STRING)
        ).build();

        FileSpec.builder(packageName, "Formatter")
                .addAnnotation(KOTLIN_FILE_ANNOTATION)
                .addType(formatter)
                .addProperty(defaultFormatter)
                .build()
                .writeTo(path);
    }
}
