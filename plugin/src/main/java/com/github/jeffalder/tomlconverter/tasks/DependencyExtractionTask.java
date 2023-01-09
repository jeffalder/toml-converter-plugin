package com.github.jeffalder.tomlconverter.tasks;

import com.github.jeffalder.tomlconverter.data.Dependency;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.DependencyConstraintSet;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskAction;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static java.util.function.Predicate.not;

/**
 * <p>This task, run on each subproject, extracts all the top-line dependencies
 * from the primary Java configurations: {@code api}, {@code implementation},
 * {@code runtimeOnly}, {@code testImplementation}, and {@code testRuntimeOnly}.
 *
 * <p>Dependencies and constraints in any other configuration will <i>not</i> be discovered.
 */
public class DependencyExtractionTask extends DefaultTask {
    private Consumer<Dependency> dependencyConsumer;

    @TaskAction
    public void action() {
        for (final var configName : List.of("api", "implementation", "runtimeOnly", "testImplementation", "testRuntimeOnly")) {
            final var config = getProject().getConfigurations().findByName(configName);
            if (config == null) {
                continue;
            }

            consumeDependencies(config.getDependencies());
            consumeConstraints(config.getDependencyConstraints());
        }
    }

    private void consumeDependencies(final DependencySet configDependencies) {
        configDependencies.stream()
                .filter(dep -> dep instanceof ExternalModuleDependency)
                .map(dep -> Dependency.from((ExternalModuleDependency)dep))
                .forEach(dependencyConsumer);
    }

    private void consumeConstraints(final DependencyConstraintSet dependencyConstraints) {
        dependencyConstraints.stream()
                .filter(not(this::isProjectDep))
                .map(Dependency::from)
                .forEach(dependencyConsumer);
    }

    // Yup, this is the best way I could find to identify it since the concrete type is internal
    private boolean isProjectDep(final DependencyConstraint foo) {
        return Arrays.stream(foo.getClass().getMethods())
                .anyMatch(m -> m.getName().equals("getProjectDependency"));
    }

    public DependencyExtractionTask setDependencyConsumer(final Consumer<Dependency> dependencyConsumer) {
        this.dependencyConsumer = dependencyConsumer;
        return this;
    }
}
