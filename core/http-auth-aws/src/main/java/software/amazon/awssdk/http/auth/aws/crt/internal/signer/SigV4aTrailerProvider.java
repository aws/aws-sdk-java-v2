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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.TrailerProvider;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class SigV4aTrailerProvider implements TrailerProvider {

    private final List<TrailerProvider> trailerProviders = new ArrayList<>();
    private final RollingSigner signer;
    private final CredentialScope credentialScope;

    public SigV4aTrailerProvider(List<TrailerProvider> trailerProviders, RollingSigner signer, CredentialScope credentialScope) {
        this.trailerProviders.addAll(trailerProviders);
        this.signer = signer;
        this.credentialScope = credentialScope;
    }

    @Override
    public void reset() {
        trailerProviders.forEach(TrailerProvider::reset);
        signer.reset();
    }

    @Override
    public Pair<String, List<String>> get() {
        byte[] trailerSig = signer.sign(getTrailers());
        return Pair.of(
            "x-amz-trailer-signature",
            Collections.singletonList(new String(trailerSig, StandardCharsets.UTF_8))
        );
    }

    private Map<String, List<String>> getTrailers() {
        // Get the headers by calling get() on each of the trailers
        return trailerProviders.stream().map(TrailerProvider::get).collect(Collectors.toMap(Pair::left, Pair::right));

    }
}
