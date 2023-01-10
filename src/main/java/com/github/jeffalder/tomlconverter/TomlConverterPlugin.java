package com.github.jeffalder.tomlconverter;

import com.github.jeffalder.tomlconverter.data.BuildGradleReplacer;
import com.github.jeffalder.tomlconverter.data.Dependency;
import com.github.jeffalder.tomlconverter.data.LibraryEntry;
import com.github.jeffalder.tomlconverter.tasks.BuildGradleRewriterTask;
import com.github.jeffalder.tomlconverter.tasks.DependencyExtractionTask;
import com.github.jeffalder.tomlconverter.tasks.ScriptWriterTask;
import com.github.jeffalder.tomlconverter.tasks.TomlWriterTask;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@NonNullApi
public class TomlConverterPlugin implements Plugin<Project> {
    public static final String BUILD_SUBDIR = "tomlConverter";

    @Override
    public void apply(final Project project) {
        final var dependencyContainer = new ConcurrentHashMap<Dependency, Boolean>();
        final Consumer<Dependency> dependencyConsumer = dep -> dependencyContainer.put(dep, true);
        final var libraryTable = new TomlTable<LibraryEntry>("libraries");
        final List<BuildGradleReplacer> replacers = new ArrayList<>();

        final var extractionTasks = project.getAllprojects().stream().map(proj ->
                        proj.getTasks().create("extractDeps", DependencyExtractionTask.class, task ->
                                task.setDependencyConsumer(dependencyConsumer)))
                .toArray();

        final var tomlWriterTask = project.getTasks().create("writeToml", TomlWriterTask.class, task ->
                task.setDependencies(dependencyContainer.keySet())
                        .setLibraryTable(libraryTable)
                        .setBuildGradleReplacerConsumer(replacers::add)
                        .dependsOn(extractionTasks));

        final var rewriterTasks = project.getAllprojects().stream().map(proj ->
                        proj.getTasks().create("rewriteBuildGradle", BuildGradleRewriterTask.class, task ->
                                task.setBuildGradleReplacers(replacers)
                                        .dependsOn(tomlWriterTask)))
                .toArray(BuildGradleRewriterTask[]::new);

        project.getTasks().create("writeConversionScript", ScriptWriterTask.class, task -> {
            task.add(tomlWriterTask)
                    .dependsOn((Object[]) rewriterTasks)
                    .dependsOn(tomlWriterTask);

            for (final var rewriterTask : rewriterTasks) {
                task.add(rewriterTask);
            }
        });
    }
}
