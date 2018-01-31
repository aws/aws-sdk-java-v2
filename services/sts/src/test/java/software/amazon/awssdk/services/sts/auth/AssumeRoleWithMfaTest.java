/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sts.auth;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.transform.AssumeRoleRequestMarshaller;

public class AssumeRoleWithMfaTest {
    @Test
    public void testMarshall() {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn("arn:aws:iam::123456789012:role/test")
                .externalId("oogeyboogey")
                .serialNumber("arn:aws:iam::123456789012:mfa/test")
                .tokenCode("000000")
                .build();

        AssumeRoleRequestMarshaller marshaller =
            new AssumeRoleRequestMarshaller();

        Request<AssumeRoleRequest> request = marshaller.marshall(assumeRoleRequest);

        Map<String, List<String>> requestParams = request.getParameters();
        Assert.assertNotNull(requestParams.get("RoleArn"));
        Assert.assertTrue(1 == requestParams.get("RoleArn").size());

        Assert.assertEquals(
            "arn:aws:iam::123456789012:role/test",
            requestParams.get("RoleArn").iterator().next());

        Assert.assertNotNull(requestParams.get("ExternalId"));
        Assert.assertTrue(1 == requestParams.get("ExternalId").size());

        Assert.assertEquals(
            "oogeyboogey",
            requestParams.get("ExternalId").iterator().next());

        Assert.assertNotNull(requestParams.get("SerialNumber"));
        Assert.assertTrue(1 == requestParams.get("SerialNumber").size());
        Assert.assertEquals(
            "arn:aws:iam::123456789012:mfa/test",
            requestParams.get("SerialNumber").iterator().next());

        Assert.assertNotNull(requestParams.get("TokenCode"));
        Assert.assertTrue(1 == requestParams.get("TokenCode").size());
        Assert.assertEquals(
            "000000",
            requestParams.get("TokenCode").iterator().next());
    }
}
