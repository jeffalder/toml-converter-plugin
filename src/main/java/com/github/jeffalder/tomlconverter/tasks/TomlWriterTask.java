package com.github.jeffalder.tomlconverter.tasks;

import com.github.jeffalder.tomlconverter.FilePreparation;
import com.github.jeffalder.tomlconverter.TomlTable;
import com.github.jeffalder.tomlconverter.data.GVACoordinates;
import com.github.jeffalder.tomlconverter.data.BuildGradleReplacer;
import com.github.jeffalder.tomlconverter.data.Dependency;
import com.github.jeffalder.tomlconverter.data.GroupVersionEntry;
import com.github.jeffalder.tomlconverter.data.LibraryEntry;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.github.jeffalder.tomlconverter.TomlConverterPlugin.BUILD_SUBDIR;
import static java.util.stream.Collectors.*;

/**
 * <p>This task accepts the list of dependencies across all subprojects,
 * makes them unique, and writes out the {@literal libs.versions.toml} file.
 * <p>In doing so, it populates the output {@link TomlTable}
 * used by later rewriter tasks.
 */
public class TomlWriterTask extends DefaultTask implements FilePreparation {
    @Override
    @Internal
    public String getTargetFile() {
        return getProject().getProjectDir().toPath()
                .resolve("gradle")
                .resolve("libs.versions.toml")
                .toAbsolutePath().toString();
    }

    @Override
    @OutputFile
    public RegularFileProperty getOutputFile() {
        return outputFile;
    }

    RegularFileProperty outputFile = getProject().getObjects().fileProperty()
            .fileValue(getProject().getBuildDir().toPath()
                    .resolve(BUILD_SUBDIR)
                    .resolve("libs.versions.toml")
                    .toFile());

    private final TomlTable<GroupVersionEntry> versionTable = new TomlTable<>("versions");

    @TaskAction
    public void writeToml() throws IOException {
        final var dependencyClassifierSets = buildClassifierGroups();

        final var sharedVersions = identifySharedVersions(dependencyClassifierSets);
        versionTable.addAll(sharedVersions);

        final Map<String, String> groupToTomlId = new HashMap<>();
        for (final var entry : versionTable) {
            groupToTomlId.put(entry.getValue().getGroup(), entry.getKey());
        }

        final var libraries = encodeLibraries(dependencyClassifierSets, groupToTomlId);
        libraryTable.addAll(libraries);

        for(final var entry : libraryTable) {
            for (final var classifier : entry.getValue().getClassifiers()) {
                consumer.accept(new BuildGradleReplacer(entry.getKey(), entry.getValue(), classifier));
            }
        }

        final var dir = outputFile.get().getAsFile().getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new GradleException("Unable to create " + dir);
        }

        try (final var output = Files.newBufferedWriter(outputFile.get().getAsFile().toPath())) {
            versionTable.write(output);
            libraryTable.write(output);
        }
    }

    private Set<GVACoordinates> buildClassifierGroups() {
        final var classifierGroups = new HashMap<GVACoordinates, Set<String>>();
        for(final var dependency : dependencies) {
            final var key = new GVACoordinates(dependency.getGroup(), dependency.getName(), dependency.getVersion());
            classifierGroups.computeIfAbsent(key, ignored -> new HashSet<>())
                    .add(dependency.getClassifier());
        }

        return classifierGroups.entrySet().stream()
                .map(entry -> entry.getKey().addClassifiers(entry.getValue()))
                .collect(toSet());
    }

    private List<LibraryEntry> encodeLibraries(final Set<GVACoordinates> dependencyClassifierSets, final Map<String, String> groupToTomlId) {
        return dependencyClassifierSets.stream()
                .map(dep -> new LibraryEntry(dep, groupToTomlId.get(dep.getGroup())))
                .collect(toUnmodifiableList());
    }

    /**
     * There's limited benefit to the shared versions section of the libs.versions.toml file.
     * I also want to avoid making the version shared <em>solely</em> due to habit. The requirements are:
     * <ol>
     *     <li>Shared versions apply to an entire group, not across groups or to a some subgroup</li>
     *     <li>Shared versions must all be explicitly stated -- don't reference a version if one wasn't referenced in build.gradle</li>
     *     <li><em>More than one</em> dependency in the group must use the version, or there's no point in "sharing"</li>
     *     <li>There must be <em>exactly one</em> unique version in the group or the prefix gets confusing</li>
     *     <li>Classifiers are ignored, so {@code foo:bar:1.0:test} and {@code foo:bar:1.0} are considered only one set of coordinates</li>
     * </ol>
     *
     * <p>This is all best-effort. I am less worried about the "best" answer, more a
     * a "good enough" answer for generated Toml.</p>
     */
    private Set<GroupVersionEntry> identifySharedVersions(final Set<GVACoordinates> dependencyClassifierSets) {
        final var allVersionsByGroup = dependencyClassifierSets.stream()
                .filter(dep -> dep.getVersion() != null)
                .map(dep -> Map.entry(dep.getGroup(), dep.getVersion()))
                .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));

        return allVersionsByGroup.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1) // there must be MORE THAN ONE reference to this group and version
                .filter(entry -> Set.copyOf(entry.getValue()).size() == 1) // there must be EXACTLY ONE unique version
                .map(entry -> new GroupVersionEntry(entry.getKey(), entry.getValue().get(0)))
                .collect(toSet());
    }

    public TomlWriterTask setDependencies(final Set<Dependency> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    private Set<Dependency> dependencies;

    public TomlWriterTask setBuildGradleReplacerConsumer(final Consumer<BuildGradleReplacer> consumer) {
        this.consumer = consumer;
        return this;
    }

    private Consumer<BuildGradleReplacer> consumer;

    public TomlWriterTask setLibraryTable(final TomlTable<LibraryEntry> libraryTable) {
        this.libraryTable = libraryTable;
        return this;
    }

    private TomlTable<LibraryEntry> libraryTable;

}
