package com.github.jeffalder.tomlconverter.data;

import com.github.jeffalder.tomlconverter.TomlTable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class LibraryEntry implements TomlTable.TomlTableRow {
    private final String group;
    private final String name;
    private final String versionKey;
    private final String version;
    private final Set<String> classifiers;

    public LibraryEntry(final GVACoordinates dependency, final String tomlId) {
        this.group = dependency.getGroup();
        this.name = dependency.getName();

        if (dependency.getVersion() == null) {
            this.versionKey = null;
            this.version = null;
        } else if (tomlId != null) {
            this.versionKey = "version.ref";
            this.version = tomlId;
        } else {
            this.versionKey = "version";
            this.version = dependency.getVersion();
        }

        this.classifiers = new HashSet<>(dependency.getClassifiers());
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getBaseId() {
        return getName();
    }

    public Set<String> getClassifiers() {
        return classifiers;
    }

    @Override
    public void write(final BufferedWriter writer) throws IOException {
        writer.write(String.format("{ module = \"%s:%s\"", group, name));
        if (versionKey != null) {
            writer.write(String.format(", %s = \"%s\"", versionKey, version));
        }
        writer.write(" }");
    }
}
