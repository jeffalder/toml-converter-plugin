package com.github.jeffalder.tomlconverter.data;

import java.util.regex.Pattern;

/**
 * This class contains the messy {@link Pattern} used to find and replace the original
 * string for the Group-Version-Artifact-Classifier (GVAC) coordinates in build.gradle[.kts]
 * and replace it with the Toml syntax. 
 */
public class BuildGradleReplacer {
    private final Pattern pattern;
    private final String replacement;

    public BuildGradleReplacer(final String tomlId, final LibraryEntry libraryEntry, final String classifier) {
        final var versionMatcher = libraryEntry.getVersion() == null ? "" : ""
                + ":"            // match the delimiting colon for the version
                + "(?:[^\"'$:]+" // match everything except the terminal quote (used for fixed versions)
                + "|"            // OR
                + "\\$\\{.*\\})";// match the Groovy interpolation format (used for properties)

        final var classifierMatcher = classifier == null ? "" : (":" + classifier);

        this.pattern = Pattern.compile(String.format("("
                + " ?\\(?"       // grab the possible starting space and/or paren
                + "[\"']"        // start with a quote
                + "%s:%s"        // group and name, maybe followed by a colon
                + versionMatcher
                + classifierMatcher
                + "[\"']"        // end with a quote
                + "\\)?"         // grab the possible ending paren
                + ")", Pattern.quote(libraryEntry.getGroup()), Pattern.quote(libraryEntry.getName())));

        final var innerReplacement = "(libs." + tomlId.replaceAll("-", ".") + ")";

        if (classifier == null) {
            replacement = innerReplacement;
        } else {
            replacement = innerReplacement + " { artifact { classifier = '" + classifier + "' } }";
        }
    }

    public String replace(final String input) {
        return pattern.matcher(input).replaceFirst(replacement);
    }
}
