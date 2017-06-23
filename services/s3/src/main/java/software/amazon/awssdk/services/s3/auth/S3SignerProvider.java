/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.auth;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import software.amazon.awssdk.auth.RegionAwareSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;
import software.amazon.awssdk.util.AwsHostNameUtils;

public class S3SignerProvider extends SignerProvider {

    private static Log log = LogFactory.getLog(S3SignerProvider.class);

    private Signer signer;

    public S3SignerProvider(final Signer defaultSigner) {
        this.signer = defaultSigner;
    }

    @Override
    public Signer getSigner(SignerProviderContext signerProviderContext) {
        URI uri = signerProviderContext.getUri();

        if (signer instanceof RegionAwareSigner) {
            // Parse region name from the host component of the URL and
            // assign it to the signer
            RegionAwareSigner regionSigner = (RegionAwareSigner) signer;

            try {
                regionSigner.setRegionName(AwsHostNameUtils.parseRegionName(
                        uri.getHost(), "s3"));

            } catch (RuntimeException e) {
                log.warn("Failed to parse the endpoint " + uri +
                        ", and skip re-assigning the signer region", e);
            }
        }

        return signer;
    }

    /**
     * This is necessary for S3 since we update the signer during request processing dependent on
     * which region the bucket exists in.
     */
    public void setSigner(Signer signer) {
        this.signer = signer;
    }

}
