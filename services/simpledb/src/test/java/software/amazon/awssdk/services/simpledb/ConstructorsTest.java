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

import software.amazon.awssdk.annotations.ReviewBeforeRelease;

/**
 * Tests that constructors provided by the SimpleDB model classes are available. This test is
 * primarily intended to help us ensure we aren't making backwards incompatible changes. We aren't
 * testing any specific behavior in the constructors, just that the constructors we expect to exist
 * are available. Backwards incompatible changes would show up as compilation errors, but it's still
 * convenient to model this a test class.
 * <p>
 * A more automated solution for detecting backwards incompatible changes would be a lot better,
 * especially considering all the service models we don't own.
 */
@ReviewBeforeRelease("Pending simple methods story")
public class ConstructorsTest {

//    @Test
//    public void testConstructors() {
//        new BatchPutAttributesRequest(null, null);
//        new CreateDomainRequest(null);
//        new DeleteAttributesRequest(null, null);
//        new DeleteDomainRequest(null);
//        new DomainMetadataRequest(null);
//        new GetAttributesRequest(null, null);
//        new PutAttributesRequest(null, null, null);
//        new SelectRequest(null);
//    }
}
