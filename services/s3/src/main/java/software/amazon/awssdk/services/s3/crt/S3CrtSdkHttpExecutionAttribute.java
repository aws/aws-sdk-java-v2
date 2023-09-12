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

package software.amazon.awssdk.services.s3.crt;


import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.http.SdkHttpExecutionAttribute;
import software.amazon.awssdk.services.s3.internal.crt.S3MetaRequestPauseObservable;

@SdkProtectedApi
public final class S3CrtSdkHttpExecutionAttribute<T> extends SdkHttpExecutionAttribute<T> {

    public static final S3CrtSdkHttpExecutionAttribute<S3MetaRequestPauseObservable> METAREQUEST_PAUSE_OBSERVABLE =
        new S3CrtSdkHttpExecutionAttribute<>(S3MetaRequestPauseObservable.class);

    public static final S3CrtSdkHttpExecutionAttribute<PublisherListener> CRT_PROGRESS_LISTENER =
        new S3CrtSdkHttpExecutionAttribute<>(PublisherListener.class);

    private S3CrtSdkHttpExecutionAttribute(Class<T> valueClass) {
        super(valueClass);
    }


}
