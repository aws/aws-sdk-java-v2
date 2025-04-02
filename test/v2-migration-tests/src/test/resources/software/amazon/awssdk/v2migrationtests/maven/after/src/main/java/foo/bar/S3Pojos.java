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

package foo.bar;

import java.util.Arrays;
import software.amazon.awssdk.services.s3.model.AccelerateConfiguration;
import software.amazon.awssdk.services.s3.model.CSVInput;
import software.amazon.awssdk.services.s3.model.CSVOutput;
import software.amazon.awssdk.services.s3.model.Condition;
import software.amazon.awssdk.services.s3.model.Destination;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.Grantee;
import software.amazon.awssdk.services.s3.model.JSONInput;
import software.amazon.awssdk.services.s3.model.JSONOutput;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.MetadataEntry;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.RedirectAllRequestsTo;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.Tag;

public class S3Pojos {

    public void s3Pojos(String bucket, String key, String id, String value) {
        AccelerateConfiguration bucketAccelerateConfiguration = AccelerateConfiguration.builder().status("Enabled")
            .build();
        MetadataEntry metadataEntry = MetadataEntry.builder().name("name").value(value)
            .build();
        Tag tag = Tag.builder().key(key).value(value)
            .build();
        Grantee canonicalGrantee = Grantee.builder().id(id)
            .build();
        Grantee emailAddressGrantee = Grantee.builder().emailAddress(id)
            .build();
        CSVInput csvInput = CSVInput.builder()
            .build();
        CSVOutput csvOutput = CSVOutput.builder()
            .build();
        JSONInput jsonInput = JSONInput.builder()
            .build();
        JSONOutput jsonOutput = JSONOutput.builder()
            .build();
        ListMultipartUploadsResponse multipartUploadListing = ListMultipartUploadsResponse.builder()
            .build();
        ListPartsResponse partListing = ListPartsResponse.builder()
            .build();
        Part partSummary = Part.builder()
            .build();
        RedirectAllRequestsTo redirectRule = RedirectAllRequestsTo.builder()
            .build();
        Destination replicationDestinationConfig = Destination.builder()
            .build();
        Condition routingRuleCondition = Condition.builder()
            .build();
        S3Object s3ObjectSummary = S3Object.builder()
            .build();
        GetBucketVersioningResponse bucketVersioningConfiguration = GetBucketVersioningResponse.builder()
            .build();
    }
}