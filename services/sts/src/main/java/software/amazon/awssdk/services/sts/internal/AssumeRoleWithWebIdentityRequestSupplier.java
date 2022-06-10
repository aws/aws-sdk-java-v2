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

package software.amazon.awssdk.services.sts.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.utils.IoUtils;

@SdkInternalApi
public class AssumeRoleWithWebIdentityRequestSupplier implements Supplier<AssumeRoleWithWebIdentityRequest> {


    private final AssumeRoleWithWebIdentityRequest request;
    private final Path webIdentityTokenFile;

    public AssumeRoleWithWebIdentityRequestSupplier(Builder builder) {

        this.request = builder.request;
        this.webIdentityTokenFile = builder.webIdentityTokenFile;

    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AssumeRoleWithWebIdentityRequest get() {
        return request.toBuilder().webIdentityToken(getToken(webIdentityTokenFile)).build();
    }

    //file extraction
    private String getToken(Path file) {
        try (InputStream webIdentityTokenStream = Files.newInputStream(file)) {
            return IoUtils.toUtf8String(webIdentityTokenStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static class Builder {

        private AssumeRoleWithWebIdentityRequest request;

        private Path webIdentityTokenFile;


        public Builder assumeRoleWithWebIdentityRequest(AssumeRoleWithWebIdentityRequest request) {
            this.request = request;
            return this;
        }

        public Builder webIdentityTokenFile(Path webIdentityTokenFile) {
            this.webIdentityTokenFile = webIdentityTokenFile;
            return this;
        }

        public AssumeRoleWithWebIdentityRequestSupplier build() {
            return new AssumeRoleWithWebIdentityRequestSupplier(this);
        }


    }
}