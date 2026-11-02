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
 * Mimics a generated modeled exception whose error code is an authentication error code (for example, STS
 * {@code ExpiredTokenException}): codegen generates an {@code isAuthenticationError()} override that returns false
 * for it.
 */
public final class TestModeledException extends TestServiceException {
    private static final long serialVersionUID = 1L;

    private TestModeledException(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public boolean isAuthenticationError() {
        return false;
    }

    public interface Builder extends TestServiceException.Builder {
        @Override
        TestModeledException build();
    }

    static final class BuilderImpl extends TestServiceException.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(TestModeledException ex) {
            super(ex);
        }

        @Override
        public TestModeledException build() {
            return new TestModeledException(this);
        }
    }
}
