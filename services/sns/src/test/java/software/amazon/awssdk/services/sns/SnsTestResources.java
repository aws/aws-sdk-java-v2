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

package software.amazon.awssdk.services.sns;

/**
 * Constants for test resource locations
 */
public class SnsTestResources {

    public static final String PACKAGE_ROOT = "/software/amazon/awssdk/services/sns/";

    /**
     * A sample notification message from SNS
     */
    public static final String SAMPLE_MESSAGE = PACKAGE_ROOT + "sample-message.json";

    /**
     * Public cert used to verify message authenticity. Fixed for unit tests.
     */
    public static final String FIXED_PUBLIC_CERT = PACKAGE_ROOT + "unit-test-public-cert.pem";
}
