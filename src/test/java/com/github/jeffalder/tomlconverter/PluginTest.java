package com.github.jeffalder.tomlconverter;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.jeffalder.tomlconverter.TomlConverterPlugin.BUILD_SUBDIR;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginTest {
    @TempDir
    public File testProjectDir;
    private File settingsFile;
    private File buildFile;

    @BeforeEach
    public void setup() {
        settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
    }

    @Test
    public void pluginTest() throws IOException {
        Files.writeString(settingsFile.toPath(), "rootProject.name = 'hello-world'");
        Files.write(buildFile.toPath(), List.of("",
                "plugins {",
                "  id('java')",
                "  id('io.github.jeffalder.tomlconverter')",
                "}",
                "",
                "dependencies {",
                // tests for the group versions (these should match and get merged
                "  implementation(\"version-group:some-artifact:1.2.3\")",
                "  testImplementation 'version-group:other-artifact:1.2.3'",
                // junit will get suffixed, but junit-bom will not
                "  implementation 'org.junit:junit:4.13.2'",
                "  implementation platform('org.junit:junit-bom:5.9.1')",
                // these two artifacts have the same name and one should be suffixed
                "  implementation \"org.junit.jupiter:junit-jupiter-api:1.2.3\"",
                "  runtimeOnly \"a-different-group:junit-jupiter-api:4.5.6\"",
                // here are classifier tests
                "  testImplementation(\"group1:name1:4.1.5\")",
                "  testRuntimeOnly(\"group1:name1:4.1.5:test\")",
                "}"
        ));

        final var result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments("writeConversionScript")
                .build();

        assertEquals(SUCCESS, result.task(":writeConversionScript").getOutcome());

        final var tomlFile = testProjectDir.toPath()
                .resolve(Path.of("build", BUILD_SUBDIR, "libs.versions.toml"));

        assertTrue(tomlFile.toFile().exists());
        final var tomlContents = Files.readString(tomlFile, StandardCharsets.UTF_8);

        verifyTomlContents(tomlContents);
        verifyBuildGradleContents(tomlContents);
    }

    private void verifyBuildGradleContents(final String tomlContents) throws IOException {
        final var newBuildGradleFile = testProjectDir.toPath()
                .resolve(Path.of("build", BUILD_SUBDIR, "build.gradle.new"));
        assertTrue(newBuildGradleFile.toFile().exists());
        final var contents = Files.readString(newBuildGradleFile, StandardCharsets.UTF_8);

        final var matcher = Pattern.compile("\n"
                        + "plugins \\{\n"
                        + "  id\\('java'\\)\n"
                        + "  id\\('io.github.jeffalder.tomlconverter'\\)\n"
                        + "}\n"
                        + "\n"
                        + "dependencies \\{\n"
                        + "  implementation\\(libs\\.some\\.artifact\\)\n"
                        + "  testImplementation\\(libs\\.other\\.artifact\\)\n"
                        + "  implementation\\(libs\\.junit([0-9a-f]{4})\\)\n"
                        + "  implementation platform\\(libs.junit.bom\\)\n"
                        + "  implementation\\(libs.junit.jupiter.api([0-9a-f]{4})?\\)\n"
                        + "  runtimeOnly\\(libs.junit.jupiter.api([0-9a-f]{4})?\\)\n"
                        + "  testImplementation\\(libs.name1\\)\n"
                        + "  testRuntimeOnly\\(libs.name1\\) \\{ artifact \\{ classifier = 'test' } }\n"
                        + "}\n"
                , Pattern.DOTALL).matcher(contents);

        assertTrue(matcher.matches());

        final var implSameName = matcher.group(2);
        final var expectedLine = "junit-jupiter-api" + (implSameName == null ? "" : implSameName)
                + " = { module = \"org.junit.jupiter:junit-jupiter-api\", version = \"1.2.3\" }";
        assertTrue(tomlContents.contains(expectedLine));

        final var runtimeSameName = matcher.group(3);
        final var runtimeLine = "junit-jupiter-api" + (runtimeSameName == null ? "" : runtimeSameName)
                + " = { module = \"a-different-group:junit-jupiter-api\", version = \"4.5.6\" }";
        assertTrue(tomlContents.contains(runtimeLine));
    }

    private void verifyTomlContents(final String tomlContents) throws IOException {
        // here are the grouped version tests
        assertTrue(
                tomlContents.contains("[versions]\nversion-group = \"1.2.3\"")
                && tomlContents.contains("other-artifact = { module = \"version-group:other-artifact\", version.ref = \"version-group\" }")
                && tomlContents.contains("some-artifact = { module = \"version-group:some-artifact\", version.ref = \"version-group\" }")
        );

        // here are the junit / junit-bom tests
        assertTrue(Pattern.compile("\njunit-bom = \\{").matcher(tomlContents).find());
        assertTrue(Pattern.compile("\njunit[0-9a-f]... = \\{").matcher(tomlContents).find());

        // here are the junit-jupiter-api tests
        assertTrue(Pattern.compile("\njunit-jupiter-api = \\{").matcher(tomlContents).find());
        assertTrue(Pattern.compile("\njunit-jupiter-api[0-9a-f]... = \\{").matcher(tomlContents).find());

        // single version
        assertEquals(1, Pattern.compile("(group1:name1)").matcher(tomlContents).results().count());
    }
}
