package com.github.jeffalder.tomlconverter;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;

public interface FilePreparation {
    @Internal
    String getTargetFile();

    @OutputFile
    RegularFileProperty getOutputFile();
}
