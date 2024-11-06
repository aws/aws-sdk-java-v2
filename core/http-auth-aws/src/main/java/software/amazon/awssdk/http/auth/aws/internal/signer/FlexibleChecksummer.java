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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.fromChecksumAlgorithm;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.readAll;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.getBinaryRequestPayloadStream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ChecksumInputStream;
import software.amazon.awssdk.http.auth.aws.internal.signer.io.ChecksumSubscriber;
import software.amazon.awssdk.utils.Validate;

/**
 * A "flexible" implementation of a checksummer. It takes a map of checksums and their header names, computes them efficiently by
 * updating each checksum while reading the payload (once), and adds the computed checksum strings to the request using the given
 * header names in the map. This should be used in cases where a (flexible) checksum algorithm is present during signing.
 */
@SdkInternalApi
public final class FlexibleChecksummer implements Checksummer {
    private final Collection<Option> options;
    private final Map<Option, SdkChecksum> optionToSdkChecksum;

    public FlexibleChecksummer(Option... options) {
        this.options = Arrays.asList(options);
        this.optionToSdkChecksum = this.options.stream().collect(
            Collectors.toMap(Function.identity(), o -> fromChecksumAlgorithm(o.algorithm))
        );
    }

    @Override
    public void checksum(ContentStreamProvider payload, SdkHttpRequest.Builder request) {
        InputStream payloadStream = getBinaryRequestPayloadStream(payload);

        ChecksumInputStream computingStream = new ChecksumInputStream(
            payloadStream,
            optionToSdkChecksum.values()
        );

        readAll(computingStream);

        addChecksums(request);
    }

    @Override
    public CompletableFuture<Publisher<ByteBuffer>> checksum(Publisher<ByteBuffer> payload, SdkHttpRequest.Builder request) {
        ChecksumSubscriber checksumSubscriber = new ChecksumSubscriber(optionToSdkChecksum.values());

        if (payload == null) {
            addChecksums(request);
            return CompletableFuture.completedFuture(null);
        }

        payload.subscribe(checksumSubscriber);
        CompletableFuture<Publisher<ByteBuffer>> result = checksumSubscriber.completeFuture();
        result.thenRun(() -> addChecksums(request));
        return result;
    }

    private void addChecksums(SdkHttpRequest.Builder request) {
        optionToSdkChecksum.forEach(
            (option, sdkChecksum) -> request.putHeader(
                option.headerName,
                option.formatter.apply(sdkChecksum.getChecksumBytes()))
        );
    }

    public static Option.Builder option() {
        return Option.builder();
    }

    public static class Option {
        private final ChecksumAlgorithm algorithm;
        private final String headerName;
        private final Function<byte[], String> formatter;

        Option(Builder builder) {
            this.algorithm = Validate.paramNotNull(builder.algorithm, "algorithm");
            this.headerName = Validate.paramNotNull(builder.headerName, "headerName");
            this.formatter = Validate.paramNotNull(builder.formatter, "formatter");
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private ChecksumAlgorithm algorithm;
            private String headerName;
            private Function<byte[], String> formatter;

            public Builder algorithm(ChecksumAlgorithm algorithm) {
                this.algorithm = algorithm;
                return this;
            }

            public Builder headerName(String headerName) {
                this.headerName = headerName;
                return this;
            }

            public Builder formatter(Function<byte[], String> formatter) {
                this.formatter = formatter;
                return this;
            }

            public Option build() {
                return new Option(this);
            }
        }
    }
}
