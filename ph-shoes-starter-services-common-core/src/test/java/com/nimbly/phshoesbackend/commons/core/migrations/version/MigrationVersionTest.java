package com.nimbly.phshoesbackend.commons.core.migrations.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MigrationVersionTest {
    @Test
    void compare_handlesDifferentSegmentLengths() {
        MigrationVersion base = MigrationVersion.parse("1.2");
        MigrationVersion withPatch = MigrationVersion.parse("1.2.0");
        MigrationVersion higherPatch = MigrationVersion.parse("1.2.3");

        assertEquals(0, base.compareTo(withPatch));
        assertEquals(-1, base.compareTo(higherPatch));
        assertEquals(1, higherPatch.compareTo(base));
    }

    @Test
    void compare_ordersNumericSegments() {
        MigrationVersion low = MigrationVersion.parse("0.0.2");
        MigrationVersion high = MigrationVersion.parse("0.0.10");

        assertEquals(-1, low.compareTo(high));
        assertEquals(1, high.compareTo(low));
    }

    @Test
    void parse_rejectsBlankSegments() {
        assertThrows(IllegalArgumentException.class, () -> MigrationVersion.parse("1..0"));
    }
}
