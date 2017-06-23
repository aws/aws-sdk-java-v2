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
import software.amazon.awssdk.services.simpledb.model.CreateDomainRequest;
import software.amazon.awssdk.services.simpledb.model.InvalidParameterValueException;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;

/**
 * Integration tests for the exceptional cases of the SimpleDB CreateDomain operation.
 *
 * @author fulghum@amazon.com
 */
public class CreateDomainIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that calling CreateDomain with an invalid domain name throws an
     * InvalidParameterValueException.
     */
    @Test
    public void testCreateDomainInvalidParameterValueException() {
        CreateDomainRequest request = CreateDomainRequest.builder()
                .domainName("''''''''''````````^^**&&@@!!??;;::[[{{]]}}||\\``''''")
                .build();
        try {
            sdb.createDomain(request);
            fail("Expected InvalidParameterValueException, but wasn't thrown");
        } catch (InvalidParameterValueException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that calling CreateDomain without specifying a domain name throws a
     * MissingParameterException.
     */
    @Test
    public void testCreateDomainMissingParameterException() {
        CreateDomainRequest request = CreateDomainRequest.builder().build();
        try {
            sdb.createDomain(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

}
