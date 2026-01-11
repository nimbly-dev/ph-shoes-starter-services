package com.nimbly.phshoesbackend.commons.core.migrations.runner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeContext;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeStep;
import com.nimbly.phshoesbackend.commons.core.migrations.version.MigrationVersionStore;

class DynamoUpgradeRunnerTest {
    @Test
    void run_skipsWhenNoSteps() {
        MigrationVersionStore versionStore = mock(MigrationVersionStore.class);
        DynamoUpgradeRunner runner = new DynamoUpgradeRunner(List.of(), mock(UpgradeContext.class), versionStore);

        runner.run(mock(ApplicationArguments.class));
    }

    @Test
    void run_ordersAppliesStepsAndUpdatesVersion() {
        UpgradeStep first = mock(UpgradeStep.class);
        UpgradeStep second = mock(UpgradeStep.class);
        UpgradeContext context = mock(UpgradeContext.class);
        MigrationVersionStore versionStore = mock(MigrationVersionStore.class);

        when(second.order()).thenReturn(0);
        when(second.service()).thenReturn("accounts_service");
        when(second.fromVersion()).thenReturn("0.0.0");
        when(second.toVersion()).thenReturn("0.0.1");
        when(second.description()).thenReturn("first");

        when(first.order()).thenReturn(1);
        when(first.service()).thenReturn("accounts_service");
        when(first.fromVersion()).thenReturn("0.0.1");
        when(first.toVersion()).thenReturn("0.0.2");
        when(first.description()).thenReturn("second");

        when(versionStore.getCurrentVersion("accounts_service")).thenReturn(Optional.of("0.0.0"));

        DynamoUpgradeRunner runner = new DynamoUpgradeRunner(List.of(first, second), context, versionStore);
        runner.run(mock(ApplicationArguments.class));

        InOrder order = inOrder(second, first, versionStore);
        order.verify(second).apply(context);
        order.verify(versionStore).saveCurrentVersion("accounts_service", "0.0.1");
        order.verify(first).apply(context);
        order.verify(versionStore).saveCurrentVersion("accounts_service", "0.0.2");
    }

    @Test
    void run_skipsStepWhenVersionAlreadyApplied() {
        UpgradeStep step = mock(UpgradeStep.class);
        UpgradeContext context = mock(UpgradeContext.class);
        MigrationVersionStore versionStore = mock(MigrationVersionStore.class);

        when(step.order()).thenReturn(0);
        when(step.service()).thenReturn("accounts_service");
        when(step.fromVersion()).thenReturn("0.0.0");
        when(step.toVersion()).thenReturn("0.0.1");
        when(step.description()).thenReturn("first");

        when(versionStore.getCurrentVersion("accounts_service")).thenReturn(Optional.of("0.0.2"));

        DynamoUpgradeRunner runner = new DynamoUpgradeRunner(List.of(step), context, versionStore);
        runner.run(mock(ApplicationArguments.class));

        verify(step, never()).apply(context);
        verify(versionStore, never()).saveCurrentVersion("accounts_service", "0.0.1");
    }
}
