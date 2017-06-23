/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.glacier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.services.glacier.model.GetVaultNotificationsRequest;
import software.amazon.awssdk.services.glacier.model.SetVaultNotificationsRequest;
import software.amazon.awssdk.services.glacier.model.VaultNotificationConfig;

public class ConfigurationIntegrationTest extends GlacierIntegrationTestBase {
    /**
     * Tests the various configuration operations on Glacier (audit logging
     * configuration, permissions and notifications).
     */
    @Test
    public void testConfigurationOperation() throws Exception {
        initializeClient();

        String topic = "arn:aws:sns:us-east-1:311841313490:topic";
        // TODO: It would be nice to have enums in the service model for event types
        String event = "ArchiveRetrievalCompleted";
        VaultNotificationConfig config = new VaultNotificationConfig().withSNSTopic(topic).withEvents(event);
        glacier.setVaultNotifications(new SetVaultNotificationsRequest().withAccountId(accountId).withVaultName(vaultName)
                                                                        .withVaultNotificationConfig(config));

        Thread.sleep(1000 * 5);

        config = glacier.getVaultNotifications(new GetVaultNotificationsRequest().withAccountId(accountId)
                                                                                 .withVaultName(vaultName))
                        .getVaultNotificationConfig();
        assertTrue(1 == config.getEvents().size());
        assertEquals(event, config.getEvents().get(0));
        assertEquals(topic, config.getSNSTopic());
    }
}
