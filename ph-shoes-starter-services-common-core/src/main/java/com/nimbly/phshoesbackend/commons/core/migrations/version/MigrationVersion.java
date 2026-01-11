package com.nimbly.phshoesbackend.commons.core.migrations.version;

import java.util.ArrayList;
import java.util.List;

public final class MigrationVersion implements Comparable<MigrationVersion> {
    private final List<Integer> segments;

    private MigrationVersion(List<Integer> segments) {
        this.segments = segments;
    }

    public static MigrationVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Migration version must be non-empty");
        }
        String[] parts = version.trim().split("\\.");
        List<Integer> parsedSegments = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isBlank()) {
                throw new IllegalArgumentException("Migration version segment is blank");
            }
            try {
                parsedSegments.add(Integer.parseInt(part));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid migration version segment: " + part, ex);
            }
        }
        return new MigrationVersion(parsedSegments);
    }

    @Override
    public int compareTo(MigrationVersion other) {
        int max = Math.max(this.segments.size(), other.segments.size());
        for (int i = 0; i < max; i++) {
            int left = i < this.segments.size() ? this.segments.get(i) : 0;
            int right = i < other.segments.size() ? other.segments.get(i) : 0;
            int diff = Integer.compare(left, right);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }
}
