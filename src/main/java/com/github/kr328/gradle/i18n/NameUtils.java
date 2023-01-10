package com.github.kr328.gradle.i18n;

public final class NameUtils {
    public static String snakeToCamel(final String input) {
        final StringBuilder builder = new StringBuilder();

        boolean shouldConvertNextCharToLower = false;
        for (final char currentChar : input.toCharArray()) {
            if (currentChar == '_') {
                shouldConvertNextCharToLower = false;
            } else if (shouldConvertNextCharToLower) {
                builder.append(Character.toLowerCase(currentChar));
            } else {
                builder.append(Character.toUpperCase(currentChar));
                shouldConvertNextCharToLower = true;
            }
        }

        return builder.toString();
    }
}
