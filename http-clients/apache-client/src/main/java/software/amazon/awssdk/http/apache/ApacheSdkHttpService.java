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

package software.amazon.awssdk.http.apache;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpService;

/**
 * Service binding for the Apache implementation.
 *
 * @deprecated Apache 4.x is in maintenance mode. Users are encouraged to switch to the
 * <a href="https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/http/apache5/Apache5HttpClient.html">Apache 5 HTTP Client</a>
 * which is feature compatible with this client.
 */
@SdkPublicApi
@Deprecated
public class ApacheSdkHttpService implements SdkHttpService {
    @Override
    public SdkHttpClient.Builder createHttpClientBuilder() {
        return ApacheHttpClient.builder();
    }
}
