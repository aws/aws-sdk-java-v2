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

package software.amazon.awssdk.services.s3.internal.crt;

import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.crt.s3.ResumeToken;
import software.amazon.awssdk.http.SdkHttpExecutionAttribute;
import software.amazon.awssdk.regions.Region;

@SdkInternalApi
public final class S3InternalSdkHttpExecutionAttribute<T> extends SdkHttpExecutionAttribute<T> {

    /**
     * The key to indicate the name of the operation
     */
    public static final S3InternalSdkHttpExecutionAttribute<String> OPERATION_NAME =
        new S3InternalSdkHttpExecutionAttribute<>(String.class);

    public static final S3InternalSdkHttpExecutionAttribute<HttpChecksum> HTTP_CHECKSUM =
        new S3InternalSdkHttpExecutionAttribute<>(HttpChecksum.class);

    public static final S3InternalSdkHttpExecutionAttribute<ResumeToken> CRT_PAUSE_RESUME_TOKEN =
        new S3InternalSdkHttpExecutionAttribute<>(ResumeToken.class);

    public static final S3InternalSdkHttpExecutionAttribute<Region> SIGNING_REGION =
        new S3InternalSdkHttpExecutionAttribute<>(Region.class);

    public static final S3InternalSdkHttpExecutionAttribute<Path> OBJECT_FILE_PATH =
        new S3InternalSdkHttpExecutionAttribute<>(Path.class);


    private S3InternalSdkHttpExecutionAttribute(Class<T> valueClass) {
        super(valueClass);
    }
}
