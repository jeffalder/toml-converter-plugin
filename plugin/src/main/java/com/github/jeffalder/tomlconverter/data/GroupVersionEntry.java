package com.github.jeffalder.tomlconverter.data;

import com.github.jeffalder.tomlconverter.TomlTable;

import java.io.BufferedWriter;
import java.io.IOException;

public class GroupVersionEntry implements TomlTable.TomlTableRow {
    private final String group;
    private final String version;

    public GroupVersionEntry(final String group, final String version) {
        this.group = group;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String getBaseId() {
        return group;
    }

    @Override
    public void write(final BufferedWriter writer) throws IOException {
        writer.write(String.format("\"%s\"", version));
    }
}
