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

package software.amazon.awssdk.services.simpledb;

import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.DeleteDomainRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;

/**
 * Integration tests for the exceptional cases of the SimpleDB DeleteDomain operation.
 *
 * @author fulghum@amazon.com
 */
public class DeleteDomainIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that the DeleteDomain operation throws a MissingParameterException if the domain name
     * isn't specified.
     */
    @Test
    public void testDeleteDomainMissingParameterException() {
        DeleteDomainRequest request = DeleteDomainRequest.builder().build();
        try {
            sdb.deleteDomain(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
