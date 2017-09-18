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

package software.amazon.awssdk.handlers;

import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;

/**
 * Callback interface for notification on web service requests executed with the
 * asynchronous clients in the AWS SDK for Java.
 */
@ReviewBeforeRelease("This doesn't seem to be used outside of SQS. Is this the correct location for it? If so, it probably "
                     + "shouldn't be in the handlers package.")
public interface AsyncHandler<REQUEST extends AmazonWebServiceRequest, RESULT> {

    /**
     * Invoked after an asynchronous request
     */
    void onError(Exception exception);

    /**
     * Invoked after an asynchronous request has completed successfully. Callers
     * have access to the original request object and the returned response
     * object.
     *
     * @param request
     *            The initial request created by the caller
     * @param result
     *            The successful result of the executed operation.
     */
    void onSuccess(REQUEST request, RESULT result);

}
