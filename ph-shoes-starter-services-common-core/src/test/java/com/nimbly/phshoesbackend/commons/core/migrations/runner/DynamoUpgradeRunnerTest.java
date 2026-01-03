package com.nimbly.phshoesbackend.commons.core.migrations.runner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;

import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeContext;
import com.nimbly.phshoesbackend.commons.core.migrations.UpgradeStep;

class DynamoUpgradeRunnerTest {

    @Test
    void run_skipsWhenNoSteps() {
        DynamoUpgradeRunner runner = new DynamoUpgradeRunner(List.of(), mock(UpgradeContext.class));

        runner.run(mock(ApplicationArguments.class));
    }

    @Test
    void run_ordersAndAppliesSteps() {
        UpgradeStep first = mock(UpgradeStep.class);
        UpgradeStep second = mock(UpgradeStep.class);
        when(first.order()).thenReturn(1);
        when(second.order()).thenReturn(0);
        UpgradeContext context = mock(UpgradeContext.class);

        DynamoUpgradeRunner runner = new DynamoUpgradeRunner(List.of(first, second), context);
        runner.run(mock(ApplicationArguments.class));

        InOrder order = inOrder(second, first);
        order.verify(second).apply(context);
        order.verify(first).apply(context);
    }
}
