package com.github.jeffalder.tomlconverter.tasks;

import com.github.jeffalder.tomlconverter.FilePreparation;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.github.jeffalder.tomlconverter.TomlConverterPlugin.BUILD_SUBDIR;

/**
 * This task has a list of {@link FilePreparation} tasks that refer to
 * a task output file, and the location where the output file should be copied to.
 * The output of this task is a UNIX shell script that copies all the task output files
 * to their expected target location.
 */
public class ScriptWriterTask extends DefaultTask {
    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    RegularFileProperty outputFile = getProject().getObjects().fileProperty()
            .fileValue(getProject().getBuildDir().toPath()
                    .resolve(BUILD_SUBDIR)
                    .resolve("convert.sh")
                    .toFile());

    private final List<FilePreparation> filePreparations = new ArrayList<>();

    @TaskAction
    public void action() throws IOException {
        final var dir = outputFile.get().getAsFile().getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new GradleException("Unable to create " + dir);
        }

        try (final var writer = Files.newBufferedWriter(outputFile.get().getAsFile().toPath())) {
            writer.write("#!/bin/sh");
            writer.newLine();
            writer.write("set -ex");
            writer.newLine();
            writer.newLine();
            for (final var filePrep : filePreparations) {
                writer.write(String.format("cp %s %s%n", filePrep.getOutputFile().get(), filePrep.getTargetFile()));
            }
        }

        if (!outputFile.get().getAsFile().setExecutable(true)) {
            throw new GradleException("Unable to make script file executable: " + outputFile.get());
        }
    }

    public ScriptWriterTask add(final FilePreparation filePreparation) {
        filePreparations.add(filePreparation);
        return this;
    }
}
