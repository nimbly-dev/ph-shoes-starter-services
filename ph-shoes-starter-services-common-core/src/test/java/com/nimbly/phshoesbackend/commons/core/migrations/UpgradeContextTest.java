package com.nimbly.phshoesbackend.commons.core.migrations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.nimbly.phshoesbackend.commons.core.config.props.DynamoMigrationProperties;

class UpgradeContextTest {

    @Test
    void tbl_returnsLogicalNameWhenNoPrefix() {
        DynamoMigrationProperties properties = new DynamoMigrationProperties();
        properties.setTablePrefix("");
        UpgradeContext context = new UpgradeContext(properties);

        assertEquals("users", context.tbl("users"));
    }

    @Test
    void tbl_prefixesWhenConfigured() {
        DynamoMigrationProperties properties = new DynamoMigrationProperties();
        properties.setTablePrefix("dev_");
        UpgradeContext context = new UpgradeContext(properties);

        assertEquals("dev_users", context.tbl("users"));
    }
}
