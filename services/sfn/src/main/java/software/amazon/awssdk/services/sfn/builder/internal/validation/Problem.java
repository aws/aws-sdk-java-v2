/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.sfn.builder.internal.validation;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Validation problem.
 */
@SdkInternalApi
final class Problem {

    private final ValidationContext context;
    private final String message;

    Problem(ValidationContext context, String message) {
        this.context = context;
        this.message = message;
    }

    /**
     * @return Context of where this violation occurred.
     */
    public ValidationContext getContext() {
        return context;
    }

    /**
     * @return Description of violation.
     */
    public String getMessage() {
        return message;
    }

}
