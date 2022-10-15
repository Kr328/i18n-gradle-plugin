package com.github.kr328.gradle.i18n;

import com.squareup.kotlinpoet.TypeName;
import com.squareup.kotlinpoet.TypeNames;
import lombok.Data;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Data
public class Template {
    private final List<Part> parts;

    private final List<Part.Variable> variables;

    public Template(final List<Part> parts) {
        this.parts = parts;
        this.variables = parts.stream()
                .filter(p -> p instanceof Part.Variable)
                .map(p -> (Part.Variable) p)
                .collect(Collectors.toList());
    }

    public static Template parse(final String template) throws ProcessorException {
        final ArrayList<Part> parts = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();

        boolean variable = false;
        boolean escape = false;
        for (final char c : template.toCharArray()) {
            if (variable) {
                if (c == '}') {
                    final String[] segments = builder.toString().split(":");
                    if (segments.length != 2) {
                        throw new ProcessorException("Unsupported variable: " + builder);
                    }

                    final String name = segments[0].trim();
                    if (!name.matches("[a-zA-Z0-9_]+")) {
                        throw new ProcessorException("Unsupported name " + name + " of " + builder);
                    }

                    final String format = segments[1].trim();
                    if (format.isEmpty()) {
                        throw new ProcessorException("Empty format of variable: " + builder);
                    }

                    final Part.Variable.FormatType type;
                    final Object testing;
                    switch (format.charAt(format.length() - 1)) {
                        case 's': {
                            type = Part.Variable.FormatType.String;
                            testing = "";
                            break;
                        }
                        case 'd': {
                            type = Part.Variable.FormatType.Decimal;
                            testing = 0;
                            break;
                        }
                        case 'o': {
                            type = Part.Variable.FormatType.Octal;
                            testing = 0;
                            break;
                        }
                        case 'x': {
                            type = Part.Variable.FormatType.Hexadecimal;
                            testing = 0;
                            break;
                        }
                        case 'f': {
                            type = Part.Variable.FormatType.Float;
                            testing = 0.0f;
                            break;
                        }
                        case 'c': {
                            type = Part.Variable.FormatType.Character;
                            testing = 'c';
                            break;
                        }
                        default: {
                            throw new ProcessorException("Unsupported format " + format + " of " + builder);
                        }
                    }

                    try {
                        if (format.equals(String.format(Locale.ROOT, format, testing))) {
                            throw new ProcessorException("Invalid format " + format + " of " + builder);
                        }
                    } catch (IllegalFormatException e) {
                        throw new ProcessorException("Invalid format " + format + " of " + builder, e);
                    }

                    parts.add(new Part.Variable(name, format, type));
                    builder.setLength(0);
                    variable = false;

                    continue;
                } else if (c == '{') {
                    throw new ProcessorException("Duplicate { of " + builder);
                }

                builder.append(c);

                continue;
            }

            if (escape) {
                switch (c) {
                    case '\\':
                        builder.append('\\');
                        break;
                    case '{':
                        builder.append('{');
                        break;
                    case '}':
                        builder.append('}');
                        break;
                    default:
                        throw new ProcessorException("Unsupported escape: " + c);
                }

                escape = false;

                continue;
            }

            if (c == '\\') {
                escape = true;

                continue;
            }

            if (c == '{') {
                if (builder.length() > 0) {
                    parts.add(new Part.Literal(builder.toString()));

                    builder.setLength(0);
                }

                variable = true;

                continue;
            }

            builder.append(c);
        }

        if (escape || variable) {
            throw new ProcessorException("Unexpected end of line: " + template);
        }

        if (builder.length() > 0) {
            parts.add(new Part.Literal(builder.toString()));
        }

        return new Template(parts);
    }

    @Override
    public String toString() {
        return parts.stream().map((p) -> {
            if (p instanceof Part.Literal) {
                return ((Part.Literal) p).text;
            } else if (p instanceof Part.Variable) {
                return "{" + ((Part.Variable) p).getName() + ": " + ((Part.Variable) p).getFormat() + "}";
            } else {
                throw new UnsupportedOperationException();
            }
        }).collect(Collectors.joining());
    }

    public interface Part {
        @Data
        final class Literal implements Part {
            private final String text;
        }

        @Data
        final class Variable implements Part {
            private final String name;
            private final String format;
            private final FormatType type;

            enum FormatType {
                String, Decimal, Octal, Hexadecimal, Float, Character;

                public TypeName kotlinType() {
                    switch (this) {
                        case String: {
                            return TypeNames.STRING;
                        }
                        case Decimal:
                        case Octal:
                        case Hexadecimal: {
                            return TypeNames.LONG;
                        }
                        case Float: {
                            return TypeNames.DOUBLE;
                        }
                        default: {
                            throw new IllegalStateException("Unexpected value: " + this);
                        }
                    }
                }
            }
        }
    }
}
