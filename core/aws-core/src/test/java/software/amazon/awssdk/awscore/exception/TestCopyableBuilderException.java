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

import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An exception that implements {@link ToCopyableBuilder} without the generated {@code isAuthenticationError()} override,
 * to show that classifying an authentication error doesn't depend on that interface.
 */
public final class TestCopyableBuilderException extends TestServiceException
    implements ToCopyableBuilder<TestCopyableBuilderException.Builder, TestCopyableBuilderException> {
    private static final long serialVersionUID = 1L;

    private TestCopyableBuilderException(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    public interface Builder extends CopyableBuilder<Builder, TestCopyableBuilderException>, TestServiceException.Builder {
        @Override
        TestCopyableBuilderException build();
    }

    static final class BuilderImpl extends TestServiceException.BuilderImpl implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(TestCopyableBuilderException ex) {
            super(ex);
        }

        @Override
        public TestCopyableBuilderException build() {
            return new TestCopyableBuilderException(this);
        }
    }
}
