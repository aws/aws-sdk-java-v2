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

import java.util.ArrayList;
import org.junit.Test;
import software.amazon.awssdk.services.simpledb.model.MissingParameterException;
import software.amazon.awssdk.services.simpledb.model.NoSuchDomainException;
import software.amazon.awssdk.services.simpledb.model.PutAttributesRequest;
import software.amazon.awssdk.services.simpledb.model.ReplaceableAttribute;

/**
 * Integration tests for the exceptional cases of the SimpleDB PutAttributes operation.
 *
 * @author fulghum@amazon.com
 */
public class PutAttributesIntegrationTest extends IntegrationTestBase {

    /**
     * Tests that PutAttributes throws a MissingParameterException when the request is missing
     * required parameters.
     */
    @Test
    public void testPutAttributesMissingParameterException() {
        PutAttributesRequest request = PutAttributesRequest.builder()
                .domainName("foo")
                .build();
        try {
            sdb.putAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }

        request = PutAttributesRequest.builder()
                .itemName("foo")
                .build();
        try {
            sdb.putAttributes(request);
            fail("Expected MissingParameterException, but wasn't thrown");
        } catch (MissingParameterException e) {
            assertValidException(e);
        }
    }

    /**
     * Tests that the PutAttributes operations throws a NoSuchDomainException if a non-existent
     * domain name is specified.
     */
    @Test
    public void testPutAttributesNoSuchDomainException() {
        PutAttributesRequest request = PutAttributesRequest.builder()
                .itemName("foobarbazbarbashbar")
                .domainName("foobarbazbarbashbar")
                .attributes(ReplaceableAttribute.builder()
                        .name("foo")
                        .value("bar")
                        .build())
                .build();
        try {
            sdb.putAttributes(request);
            fail("Expected NoSuchDomainException, but wasn't thrown");
        } catch (NoSuchDomainException e) {
            assertValidException(e);
        }
    }

}
