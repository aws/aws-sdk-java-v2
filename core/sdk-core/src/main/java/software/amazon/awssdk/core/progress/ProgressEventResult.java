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

package software.amazon.awssdk.core.progress;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Progress event result.
 */
@SdkPublicApi
public interface ProgressEventResult {

    /**
     * @return an empty result
     */
    static ProgressEventResult empty() {
        return EmptyProgressEventResult.instance();
    }

    final class EmptyProgressEventResult implements ProgressEventResult {
        private static final EmptyProgressEventResult INSTANCE = new EmptyProgressEventResult();

        private EmptyProgressEventResult() {
        }

        static ProgressEventResult instance() {
            return INSTANCE;
        }
    }
}
