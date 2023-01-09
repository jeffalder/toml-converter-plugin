package com.github.jeffalder.tomlconverter.data;

import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.util.Objects;

/**
 * This class represents a unique set of Group-Version-Artifact-Classifier (GVAC)
 * coordinates used somewhere in all the projects.
 */
public class Dependency {
    private final String group;
    private final String name;
    private final String version;
    private final String classifier;

    private Dependency(final String group, final String name, final String version, final String classifier) {
        Objects.requireNonNull(group);
        Objects.requireNonNull(name);
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
    }

    public static Dependency from(final ExternalModuleDependency externalModuleDependency) {
        return new Dependency(externalModuleDependency.getGroup(), externalModuleDependency.getName(), externalModuleDependency.getVersion(),
                externalModuleDependency.getArtifacts().stream().findFirst().map(DependencyArtifact::getClassifier).orElse(null));
    }

    public static Dependency from(final DependencyConstraint constraint) {
        return new Dependency(constraint.getGroup(), constraint.getName(), constraint.getVersion(), null);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Dependency that = (Dependency) o;
        return group.equals(that.group) && name.equals(that.name) && Objects.equals(version, that.version) && Objects.equals(classifier,
                that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version, classifier);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "group='" + group + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", classifier='" + classifier + '\'' +
                '}';
    }

    public String getClassifier() {
        return classifier;
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
}
