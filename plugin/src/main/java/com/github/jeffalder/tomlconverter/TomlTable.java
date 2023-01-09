package com.github.jeffalder.tomlconverter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * This implementation of a TomlTable does a few things for us:
 * <ul>
 *     <li>It will accept a preferred ID and translate it into a valid TOML key</li>
 *     <li>It understands the code generation limitations of Gradle and attempts to work around them</li>
 *     <li>It writes the Toml format to a {@link BufferedWriter}</li>
 * </ul>
 *
 * <p>The code generation limitations of gradle are that one key cannot vary from another solely by a separator and suffix.
 * For example, gradle code generation will not work properly if you have, say, "junit" and "junit-bom" prefixes.
 * In that case, {@code libs.junit} can either refer to a dependency <em>or</em> something that contains "getBom()",
 * but not both. This code works around this by adding a random suffix if it detects a conflict, yielding "junita123" and "junit-bom".
 * @param <T>
 */
public class TomlTable<T extends TomlTable.TomlTableRow> implements Iterable<Map.Entry<String, T>> {
    private final String tableName;
    private final Map<String, T> rows = new HashMap<>();

    public TomlTable(final String tableName) {
        this.tableName = tableName;
    }

    public void addAll(final Collection<T> rows) {
        rows.stream()
                .sorted(comparing(TomlTableRow::getBaseId).reversed())
                .forEach(this::add);
    }

    public TomlTable<T> add(final T row) {
        final var initialId = row.getBaseId()
                .replaceAll("[^a-zA-Z0-9]+", " ") // strip invalid characters
                .strip()                          // may have been invalid characters on either end
                .replaceAll(" ([0-9])", "$1")     // remove space before any leading digits
                .replaceAll(" ", "-");            // swap back to dashes

        final var tomlId = rows.containsKey(initialId) || rows.keySet().stream().anyMatch(id -> id.startsWith(initialId + "-"))
                // another library is already assigned to this ID, or it has a suffix.
                // we must modify the final component for (hopefully) unambiguous matches.
                ? initialId + String.format("%04x", ThreadLocalRandom.current().nextInt(1<<16))
                : initialId;

        rows.put(tomlId, row);

        return this;
    }

    public void write(final BufferedWriter writer) throws IOException {
        writer.write("[" + tableName + "]");
        writer.newLine();
        for (final var entry : rows.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toUnmodifiableList())) {
            writer.write(entry.getKey() + " = ");
            entry.getValue().write(writer);
            writer.newLine();
        }
        writer.newLine();
    }

    @Override
    public Iterator<Map.Entry<String, T>> iterator() {
        return rows.entrySet().iterator();
    }

    public interface TomlTableRow {
        String getBaseId();

        void write(BufferedWriter writer) throws IOException;
    }
}
