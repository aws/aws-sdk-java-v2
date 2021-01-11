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

package software.amazon.awssdk.stability.tests;


/**
 * The main method will be invoked when you execute the test jar generated from
 * "mvn package -P test-jar"
 *
 * You can add the tests in the main method.
 * eg:
 *        try {
 *            S3AsyncStabilityTest s3AsyncStabilityTest = new S3AsyncStabilityTest();
 *            S3AsyncStabilityTest.setup();
 *            s3AsyncStabilityTest.putObject_getObject();
 *        } finally {
 *            S3AsyncStabilityTest.cleanup();
 *        }
 */
public class TestRunner {

    public static void main(String... args) {
    }
}
