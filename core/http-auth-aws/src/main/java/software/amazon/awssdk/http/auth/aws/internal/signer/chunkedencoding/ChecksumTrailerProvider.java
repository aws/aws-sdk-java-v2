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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class ChecksumTrailerProvider implements TrailerProvider {

    private final SdkChecksum checksum;
    private final String checksumName;

    public ChecksumTrailerProvider(SdkChecksum checksum, String checksumName) {
        this.checksum = checksum;
        this.checksumName = checksumName;
    }

    @Override
    public void reset() {
        checksum.reset();
    }

    @Override
    public Pair<String, List<String>> get() {
        return Pair.of(
            checksumName,
            Collections.singletonList(BinaryUtils.toBase64(checksum.getChecksumBytes()))
        );
    }
}
