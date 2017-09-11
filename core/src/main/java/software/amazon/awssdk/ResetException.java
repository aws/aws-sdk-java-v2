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

package software.amazon.awssdk;

/**
 * Stream reset failure.
 */
public class ResetException extends SdkClientException {
    private static final long serialVersionUID = 1L;
    private String extraInfo;

    public ResetException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * {@inheritDoc}
     * A stream reset exception cannot be retried.
     */
    @Override
    public boolean isRetryable() {
        return false;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        return extraInfo == null ? msg : msg + ";  " + extraInfo;
    }
}
