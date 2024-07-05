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

package software.amazon.awssdk.core.http;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

@SdkInternalApi
@Immutable
public class NoopHttpFullRequest implements SdkHttpFullRequest {

    public void NoopHttpFullRequest() {

    }
    @Override
    public Builder toBuilder() {
        return null;
    }

    @Override
    public Optional<ContentStreamProvider> contentStreamProvider() {
        return Optional.empty();
    }

    @Override
    public Map<String, List<String>> headers() {
        return null;
    }

    @Override
    public String protocol() {
        return null;
    }

    @Override
    public String host() {
        return null;
    }

    @Override
    public int port() {
        return 0;
    }

    @Override
    public String encodedPath() {
        return null;
    }

    @Override
    public Map<String, List<String>> rawQueryParameters() {
        return null;
    }

    @Override
    public SdkHttpMethod method() {
        return null;
    }
}
