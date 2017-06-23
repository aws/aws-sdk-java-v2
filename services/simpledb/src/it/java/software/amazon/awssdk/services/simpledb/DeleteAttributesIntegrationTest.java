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
import software.amazon.awssdk.services.simpledb.model.DeleteAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;

/**
 * Integration tests for the exceptional cases of the SimpleDB DeleteAttributes operation.
 *
 * @author fulghum@amazon.com
 */
public class DeleteAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that calling the DeleteAttribute operation without a domain name, or item name
     * specified throws a MissingParameterException.
     */
    @Test
    public void testDeleteAttributesMissingParameterException() {
        DeleteAttributesRequest request = DeleteAttributesRequest.builder()
                .itemName("foo")
                .build();
        try {
            sdb.deleteAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        request = DeleteAttributesRequest.builder()
                .domainName("foo")
                .build();
        try {
            sdb.deleteAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that calling the DeleteAttributes operation with a non-existent domain name throws a
     * NoSuchDomainException.
     */
    @Test
    public void testDeleteAttributesNoSuchDomainException() {
        DeleteAttributesRequest request = DeleteAttributesRequest.builder()
                .domainName("foobarbazbarbashbarbazfoo")
                .itemName("foobarbazbarbashbarbazfoo")
                .build();
        try {
            sdb.deleteAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }
}
