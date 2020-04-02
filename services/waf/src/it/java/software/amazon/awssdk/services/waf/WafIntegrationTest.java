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

package software.amazon.awssdk.services.waf;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.waf.model.ChangeAction;
import software.amazon.awssdk.services.waf.model.CreateIpSetRequest;
import software.amazon.awssdk.services.waf.model.CreateIpSetResponse;
import software.amazon.awssdk.services.waf.model.GetChangeTokenRequest;
import software.amazon.awssdk.services.waf.model.GetChangeTokenResponse;
import software.amazon.awssdk.services.waf.model.GetIpSetRequest;
import software.amazon.awssdk.services.waf.model.GetIpSetResponse;
import software.amazon.awssdk.services.waf.model.IPSet;
import software.amazon.awssdk.services.waf.model.IPSetDescriptor;
import software.amazon.awssdk.services.waf.model.IPSetDescriptorType;
import software.amazon.awssdk.services.waf.model.IPSetUpdate;
import software.amazon.awssdk.services.waf.model.ListIpSetsRequest;
import software.amazon.awssdk.services.waf.model.ListIpSetsResponse;
import software.amazon.awssdk.services.waf.model.UpdateIpSetRequest;
import software.amazon.awssdk.services.waf.model.WafNonEmptyEntityException;
import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class WafIntegrationTest extends AwsTestBase {

    private static final String IP_SET_NAME = "java-sdk-ipset-" + System.currentTimeMillis();
    private static final String IP_ADDRESS_RANGE = "192.0.2.0/24";
    private static WafClient client = null;
    private static String ipSetId = null;

    @BeforeClass
    public static void setup() throws IOException {
        FixedDelayBackoffStrategy fixedBackoffStrategy = FixedDelayBackoffStrategy.create(Duration.ofSeconds(30));
        setUpCredentials();
        client = WafClient.builder()
                          .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                          .region(Region.AWS_GLOBAL)
                          .overrideConfiguration(cfg -> cfg.retryPolicy(r -> r.backoffStrategy(fixedBackoffStrategy)))
                          .build();
    }

    @AfterClass
    public static void tearDown() throws IOException {
        if (client != null) {

            deleteIpSet();
        }
    }

    private static void deleteIpSet() {
        if (ipSetId != null) {
            Waiter.run(() -> client.deleteIPSet(r -> r.ipSetId(ipSetId).changeToken(newChangeToken())))
                  .ignoringException(WafNonEmptyEntityException.class)
                  .orFailAfter(Duration.ofMinutes(2));
        }
    }

    private static String newChangeToken() {

        GetChangeTokenResponse result = client.getChangeToken(GetChangeTokenRequest.builder().build());
        return result.changeToken();

    }

    @Test
    public void testOperations() throws InterruptedException {

        ipSetId = testCreateIpSet();
        testGetIpSet();
        testUpdateIpSet();
    }

    private String testCreateIpSet() {
        final String changeToken = newChangeToken();
        CreateIpSetResponse createResult = client.createIPSet(CreateIpSetRequest.builder()
                                                                                .changeToken(changeToken)
                                                                                .name(IP_SET_NAME).build());

        Assert.assertEquals(changeToken, createResult.changeToken());

        final IPSet ipSet = createResult.ipSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.name());
        Assert.assertTrue(ipSet.ipSetDescriptors().isEmpty());
        Assert.assertNotNull(ipSet.ipSetId());

        return ipSet.ipSetId();
    }

    private void testGetIpSet() {
        GetIpSetResponse getResult = client.getIPSet(GetIpSetRequest.builder()
                .ipSetId(ipSetId)
                .build());
        IPSet ipSet = getResult.ipSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.name());
        Assert.assertTrue(ipSet.ipSetDescriptors().isEmpty());
        Assert.assertNotNull(ipSet.ipSetId());
        Assert.assertEquals(ipSetId, ipSet.ipSetId());

        ListIpSetsResponse listResult = client.listIPSets(ListIpSetsRequest.builder()
                                                                           .limit(1)
                                                                           .build());
        Assert.assertNotNull(listResult.ipSets());
        Assert.assertFalse(listResult.ipSets().isEmpty());
    }

    private void testUpdateIpSet() {
        final IPSetDescriptor ipSetDescriptor = IPSetDescriptor.builder()
                .type(IPSetDescriptorType.IPV4)
                .value(IP_ADDRESS_RANGE)
                .build();
        final IPSetUpdate ipToInsert = IPSetUpdate.builder()
                .ipSetDescriptor(ipSetDescriptor)
                .action(ChangeAction.INSERT)
                .build();


        client.updateIPSet(UpdateIpSetRequest.builder()
                                             .ipSetId(ipSetId)
                                             .changeToken(newChangeToken())
                                             .updates(ipToInsert).build());
        GetIpSetResponse getResult = client.getIPSet(GetIpSetRequest.builder()
                                                                    .ipSetId(ipSetId).build());

        IPSet ipSet = getResult.ipSet();
        Assert.assertNotNull(ipSet);
        Assert.assertEquals(IP_SET_NAME, ipSet.name());
        Assert.assertNotNull(ipSet.ipSetId());
        Assert.assertEquals(ipSetId, ipSet.ipSetId());

        List<IPSetDescriptor> actualList = ipSet.ipSetDescriptors();

        Assert.assertFalse(actualList.isEmpty());
        Assert.assertEquals(1, actualList.size());
        IPSetDescriptor actualIpSetDescriptor = actualList.get(0);
        Assert.assertEquals(ipSetDescriptor.type(), actualIpSetDescriptor.type());
        Assert.assertEquals(ipSetDescriptor.value(), actualIpSetDescriptor.value());

        final IPSetUpdate ipToDelete = IPSetUpdate.builder()
                .ipSetDescriptor(ipSetDescriptor)
                .action(ChangeAction.DELETE)
                .build();

        client.updateIPSet(UpdateIpSetRequest.builder()
                .ipSetId(ipSetId)
                .changeToken(newChangeToken())
                .updates(ipToDelete)
                .build());
    }
}
