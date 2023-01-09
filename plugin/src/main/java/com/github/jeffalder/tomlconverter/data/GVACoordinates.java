package com.github.jeffalder.tomlconverter.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This object is a key-value pair where the key is the group-version-name coordinates that are
 * specified in the Toml file (aka GVA, Group-Version-Artifact). Gradle Toml doesn't contain
 * classifiers, so this class is used to uniquely identify libraries in the Toml file
 * and accumulate all the various classifiers that are used for those GVA coordinates.
 */
public class GVACoordinates {
    private final String group;
    private final String name;
    private final String version;
    private final Set<String> classifiers = new HashSet<>();

    public GVACoordinates(final String group, final String name, final String version) {
        Objects.requireNonNull(group);
        Objects.requireNonNull(name);

        this.group = group;
        this.name = name;
        this.version = version;
    }

    public GVACoordinates addClassifiers(final Collection<String> collection) {
        classifiers.addAll(collection);
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final GVACoordinates that = (GVACoordinates) o;
        return group.equals(that.group) && name.equals(that.name) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version);
    }

    public String getVersion() {
        return version;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public Set<String> getClassifiers() {
        return classifiers;
    }
}
