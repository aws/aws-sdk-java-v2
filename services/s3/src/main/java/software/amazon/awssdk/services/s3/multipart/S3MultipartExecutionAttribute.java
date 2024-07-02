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

package software.amazon.awssdk.services.s3.multipart;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;

@SdkProtectedApi
public final class S3MultipartExecutionAttribute extends SdkExecutionAttribute {
    public static final ExecutionAttribute<S3ResumeToken> RESUME_TOKEN = new ExecutionAttribute<>("ResumeToken");
    public static final ExecutionAttribute<PauseObservable> PAUSE_OBSERVABLE = new ExecutionAttribute<>("PauseObservable");
    public static final ExecutionAttribute<PublisherListener<Long>> JAVA_PROGRESS_LISTENER =
        new ExecutionAttribute<>("JavaProgressListener");
    public static final ExecutionAttribute<MultipartDownloadResumeContext> MULTIPART_DOWNLOAD_RESUME_CONTEXT =
        new ExecutionAttribute<>("MultipartDownloadResumeContext");
}
