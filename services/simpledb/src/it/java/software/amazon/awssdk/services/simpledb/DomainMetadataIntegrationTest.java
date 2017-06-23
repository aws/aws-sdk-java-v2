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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.services.simpledb.model.DomainMetadataRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;

/**
 * Integration tests for the exceptional cases of the SimpleDB DomainMetadata operation.
 *
 * @author fulghum@amazon.com
 */
public class DomainMetadataIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that a NoSuchDomainException is thrown when the client calls the domainMetadata service
     * method with a non-existent domain name.
     */
    @Test
    @ReviewBeforeRelease("BoxUsage unmarshalling broken by model refactor")
    public void testDomainMetadataInvalidParameterException() {
        final String imaginaryDomainName = "AnImaginaryDomainNameThatDoesntExist";

        DomainMetadataRequest request = DomainMetadataRequest.builder()
                                                             .domainName(imaginaryDomainName)
                                                             .build();

        try {
            sdb.domainMetadata(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);

            // TODO Fix box usage
            // assertNotNull(e.boxUsage());
            // assertTrue(e.boxUsage().floatValue() > 0);

            assertEquals(400, e.getStatusCode());
        }
    }

    /**
     * Tests that a MissingParameterException is thrown when the client calls the domainMetadata
     * service method without specifying a domain name.
     */
    @Test
    @ReviewBeforeRelease("BoxUsage unmarshalling broken by model refactor")
    public void testDomainMetadataMissingParameterException() {
        DomainMetadataRequest request = DomainMetadataRequest.builder().build();

        try {
            sdb.domainMetadata(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
            // TODO fix box usage
            // assertNotNull(e.boxUsage());
            // assertTrue(e.boxUsage().floatValue() > 0);

            assertEquals(400, e.getStatusCode());
        }
    }

}
