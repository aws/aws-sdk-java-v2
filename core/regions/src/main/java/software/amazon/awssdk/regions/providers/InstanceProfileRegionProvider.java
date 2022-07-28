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

package software.amazon.awssdk.regions.providers;

import java.io.IOException;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.imds.Ec2Metadata;
import software.amazon.awssdk.imds.MetadataResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Logger;

/**
 * Attempts to load region information from the EC2 Metadata service. If the application is not
 * running on EC2 this provider will thrown an exception.
 *
 * <P>
 * If {@link SdkSystemSetting#AWS_EC2_METADATA_DISABLED} is set to true, it will not try to load
 * region from EC2 metadata service and will return null.
 */
@SdkProtectedApi
public final class InstanceProfileRegionProvider implements AwsRegionProvider {

    private static final Logger log = Logger.loggerFor(InstanceProfileRegionProvider.class);

    private static final String REGION = "region";

    private static final String EC2_DYNAMICDATA_ROOT = "/latest/dynamic/";

    private static final String INSTANCE_IDENTITY_DOCUMENT = "instance-identity/document";

    /**
     * Cache region as it will not change during the lifetime of the JVM.
     */
    private volatile String region;

    @Override
    public Region getRegion() throws SdkClientException {
        if (SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow()) {
            throw SdkClientException.builder()
                                    .message("EC2 Metadata is disabled. Unable to retrieve region information from " +
                                             "EC2 Metadata service.")
                                    .build();
        }

        if (region == null) {
            synchronized (this) {
                if (region == null) {
                    this.region = tryDetectRegion();
                }
            }
        }

        if (region == null) {
            throw SdkClientException.builder()
                                    .message("Unable to retrieve region information from EC2 Metadata service. "
                                         + "Please make sure the application is running on EC2.")
                                    .build();
        }

        return Region.of(region);
    }

    private String tryDetectRegion() {

        Ec2Metadata ec2Metadata = Ec2Metadata.create();
        MetadataResponse metadataResponse = ec2Metadata.get(EC2_DYNAMICDATA_ROOT + INSTANCE_IDENTITY_DOCUMENT);

        try {
            Document document = metadataResponse.asDocument();
            if (document.isMap()) {
                Map<String, Document> documentMap = document.asMap();
                Document regionDocument = documentMap.get(REGION);
                return regionDocument.asString();
            }
        } catch (IOException e) {
            log.warn(() -> "Received IOException", e);
        }
        return null;
    }
}
