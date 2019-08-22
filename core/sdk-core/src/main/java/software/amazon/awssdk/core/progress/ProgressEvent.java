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
 * A progress event. Typically this is used to notify a chunk of bytes has been
 * transferred. This can also used to notify other types of progress events such as a
 * transfer starting, or failing.
 *
 * @see ProgressEventListener
 * @see ProgressEventType
 */
@SdkPublicApi
public interface ProgressEvent {

    /**
     * @return the type of the progress event
     */
    ProgressEventType eventType();

    /**
     * @return the optional event data
     */
    ProgressEventData eventData();
}
