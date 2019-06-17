/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Model to store the test result
 */
public class TestResult {
    private final String testName;
    private final int totalRequestCount;
    private final int serviceExceptionCount;
    private final int ioExceptionCount;
    private final int clientExceptionCount;
    private final int unknownExceptionCount;

    private TestResult(Builder builder) {
        this.testName = builder.testName;
        this.totalRequestCount = builder.totalRequestCount;
        this.serviceExceptionCount = builder.serviceExceptionCount;
        this.ioExceptionCount = builder.ioExceptionCount;
        this.clientExceptionCount = builder.clientExceptionCount;
        this.unknownExceptionCount = builder.unknownExceptionCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String testName() {
        return testName;
    }

    public int totalRequestCount() {
        return totalRequestCount;
    }

    public int serviceExceptionCount() {
        return serviceExceptionCount;
    }

    public int ioExceptionCount() {
        return ioExceptionCount;
    }

    public int clientExceptionCount() {
        return clientExceptionCount;
    }

    public int unknownExceptionCount() {
        return unknownExceptionCount;
    }

    @Override
    public String toString() {
        return "{" +
               "testName: '" + testName + '\'' +
               ", totalRequestCount: " + totalRequestCount +
               ", serviceExceptionCount: " + serviceExceptionCount +
               ", ioExceptionCount: " + ioExceptionCount +
               ", clientExceptionCount: " + clientExceptionCount +
               ", unknownExceptionCount: " + unknownExceptionCount +
               '}';
    }

    public static class Builder {
        private String testName;
        private int totalRequestCount;
        private int serviceExceptionCount;
        private int ioExceptionCount;
        private int clientExceptionCount;
        private int unknownExceptionCount;

        private Builder() {
        }

        public Builder testName(String testName) {
            this.testName = testName;
            return this;
        }

        public Builder totalRequestCount(int totalRequestCount) {
            this.totalRequestCount = totalRequestCount;
            return this;
        }

        public Builder serviceExceptionCount(int serviceExceptionCount) {
            this.serviceExceptionCount = serviceExceptionCount;
            return this;
        }

        public Builder ioExceptionCount(int ioExceptionCount) {
            this.ioExceptionCount = ioExceptionCount;
            return this;
        }

        public Builder clientExceptionCount(int clientExceptionCount) {
            this.clientExceptionCount = clientExceptionCount;
            return this;
        }

        public Builder unknownExceptionCount(int unknownExceptionCount) {
            this.unknownExceptionCount = unknownExceptionCount;
            return this;
        }

        public TestResult build() {
            return new TestResult(this);
        }
    }
}
