/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BatchOverrideConfigurationTest {

    private BatchOverrideConfiguration overrideConfiguration;
    private ScheduledExecutorService scheduledExecutor;
    private final int maxBatchItems = 10;
    private final int maxBatchOpenInMs = 200;

    @Before
    public void setUp() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        overrideConfiguration = BatchOverrideConfiguration.builder()
                                                          .maxBatchItems(maxBatchItems)
                                                          .maxBatchOpenInMs(Duration.ofMillis(maxBatchOpenInMs))
                                                          .scheduledExecutor(scheduledExecutor)
                                                          .build();
    }

    @After
    public void tearDown() {
        scheduledExecutor.shutdownNow();
    }

    @Test
    public void createNewBatchOverrideConfiguration() {
        Assert.assertEquals(maxBatchItems, overrideConfiguration.maxBatchItems().intValue());
        Assert.assertEquals(maxBatchOpenInMs, overrideConfiguration.maxBatchOpenInMs().toMillis());
        Assert.assertEquals(scheduledExecutor, overrideConfiguration.scheduledExecutor());
    }

    @Test
    public void creatingCopyWithToBuilderAndCheckEqual() {
        BatchOverrideConfiguration overrideConfigurationCopy = overrideConfiguration.toBuilder().build();
        Assert.assertEquals(maxBatchItems, overrideConfigurationCopy.maxBatchItems().intValue());
        Assert.assertEquals(maxBatchOpenInMs, overrideConfigurationCopy.maxBatchOpenInMs().toMillis());
        Assert.assertEquals(scheduledExecutor, overrideConfigurationCopy.scheduledExecutor());
        Assert.assertEquals(overrideConfiguration, overrideConfigurationCopy);
        Assert.assertEquals(overrideConfiguration.hashCode(), overrideConfigurationCopy.hashCode());
    }

    @Test
    public void toStringMethod() {
        String stringRepresentation = overrideConfiguration.toString();
        String expected = String.format("BatchOverrideConfiguration(maxBatchItems=%d, maxBatchOpenInMs=%d)", maxBatchItems,
                                        maxBatchOpenInMs);
        Assert.assertEquals(expected, stringRepresentation);
    }
}
