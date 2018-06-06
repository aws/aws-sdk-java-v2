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

package software.amazon.awssdk.awscore.config.options;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.config.options.ClientOption;
import software.amazon.awssdk.regions.Region;

@SdkProtectedApi
public class AwsClientOption<T> extends ClientOption<T> {
    /**
     * @see AwsClientBuilder#credentialsProvider(AwsCredentialsProvider)
     */
    public static final AwsClientOption<AwsCredentialsProvider> CREDENTIALS_PROVIDER =
            new AwsClientOption<>(AwsCredentialsProvider.class);

    /**
     * AWS Region the client was configured with. Note that this is not always the signing region in the case of global
     * services like IAM.
     */
    public static final AwsClientOption<Region> AWS_REGION = new AwsClientOption<>(Region.class);

    /**
     * AWS Region to be used for signing the request. This is not always same as {@link #AWS_REGION} in case of global services.
     */
    public static final AwsClientOption<Region> SIGNING_REGION = new AwsClientOption<>(Region.class);

    public static final AwsClientOption<String> SERVICE_SIGNING_NAME = new AwsClientOption<>(String.class);

    protected AwsClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
