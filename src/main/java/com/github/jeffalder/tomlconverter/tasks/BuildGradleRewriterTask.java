package com.github.jeffalder.tomlconverter.tasks;

import com.github.jeffalder.tomlconverter.FilePreparation;
import com.github.jeffalder.tomlconverter.TomlConverterPlugin;
import com.github.jeffalder.tomlconverter.data.BuildGradleReplacer;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This task, run in each subproject, reads the build.gradle[.kts] file,
 * transforms each line to use the toml replacement (if it can),
 * and writes the resulting line to build/build.gradle[.kts].new.
 */
public class BuildGradleRewriterTask extends DefaultTask implements FilePreparation {

    @Internal
    @Override
    public String getTargetFile() {
        return getProject().getBuildFile().getAbsolutePath();
    }

    @OutputFile
    @Override
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    RegularFileProperty outputFile = getProject().getObjects().fileProperty()
            .fileValue(getProject().getBuildDir().toPath()
                    .resolve(TomlConverterPlugin.BUILD_SUBDIR)
                    .resolve(getProject().getBuildFile().getName() + ".new")
                    .toFile());

    @TaskAction
    public void action() throws IOException {
        final var dir = outputFile.get().getAsFile().getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new GradleException("Unable to create " + dir);
        }

        try (
                final var reader = Files.newBufferedReader(getProject().getBuildFile().toPath());
                final var writer = Files.newBufferedWriter(outputFile.get().getAsFile().toPath())
        ) {
            for(var line : reader.lines().collect(Collectors.toUnmodifiableList())) {
                for(final var replacer : buildGradleReplacers) {
                    line = replacer.replace(line);
                }

                writer.write(line);
                writer.newLine();
            }
        }
    }

    public BuildGradleRewriterTask setBuildGradleReplacers(final List<BuildGradleReplacer> buildGradleReplacers) {
        this.buildGradleReplacers = buildGradleReplacers;
        return this;
    }
    private List<BuildGradleReplacer> buildGradleReplacers;
}
