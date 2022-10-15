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

    public static String lowerCamelToUpper(final String input) {
        if (input.length() == 0) {
            return "";
        }

        char firstChar = Character.toUpperCase(input.charAt(0));
        return firstChar + input.substring(1);
    }

    public static String upperCamelToLower(final String input) {
        if (input.length() == 0) {
            return "";
        }

        char firstChar = Character.toLowerCase(input.charAt(0));
        return firstChar + input.substring(1);
    }
}
