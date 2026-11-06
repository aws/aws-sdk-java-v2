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

package software.amazon.awssdk.awscore.exception;

/**
 * Mimics a generated service base exception (for example, {@code StsException}): the class the SDK builds for errors that
 * the service model doesn't declare.
 */
public class TestServiceException extends AwsServiceException {
    private static final long serialVersionUID = 1L;

    protected TestServiceException(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends AwsServiceException.Builder {
        @Override
        TestServiceException build();
    }

    protected static class BuilderImpl extends AwsServiceException.BuilderImpl implements Builder {
        protected BuilderImpl() {
        }

        protected BuilderImpl(TestServiceException ex) {
            super(ex);
        }

        @Override
        public TestServiceException build() {
            return new TestServiceException(this);
        }
    }
}
