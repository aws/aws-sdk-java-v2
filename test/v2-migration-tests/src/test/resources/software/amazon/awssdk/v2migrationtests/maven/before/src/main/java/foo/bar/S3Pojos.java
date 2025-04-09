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

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketAccelerateConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CSVInput;
import com.amazonaws.services.s3.model.CSVOutput;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.EmailAddressGrantee;
import com.amazonaws.services.s3.model.JSONInput;
import com.amazonaws.services.s3.model.JSONOutput;
import com.amazonaws.services.s3.model.MetadataEntry;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.PartitionDateSource;
import com.amazonaws.services.s3.model.PartitionedPrefix;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.RedirectRule;
import com.amazonaws.services.s3.model.ReplicationDestinationConfig;
import com.amazonaws.services.s3.model.RoutingRuleCondition;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.Tag;
import java.util.Arrays;

public class S3Pojos {

    public void s3Pojos(String bucket, String key, String id, String value) {
        BucketAccelerateConfiguration bucketAccelerateConfiguration = new BucketAccelerateConfiguration("Enabled");
        MetadataEntry metadataEntry = new MetadataEntry("name", value);
        Tag tag = new Tag(key, value);
        CanonicalGrantee canonicalGrantee = new CanonicalGrantee(id);
        EmailAddressGrantee emailAddressGrantee = new EmailAddressGrantee(id);
        CSVInput csvInput = new CSVInput();
        CSVOutput csvOutput = new CSVOutput();
        JSONInput jsonInput = new JSONInput();
        JSONOutput jsonOutput = new JSONOutput();
        MultipartUploadListing multipartUploadListing = new MultipartUploadListing();
        PartListing partListing = new PartListing();
        PartSummary partSummary = new PartSummary();
        RedirectRule redirectRule = new RedirectRule();
        ReplicationDestinationConfig replicationDestinationConfig = new ReplicationDestinationConfig();
        RoutingRuleCondition routingRuleCondition = new RoutingRuleCondition();
        S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        BucketVersioningConfiguration bucketVersioningConfiguration = new BucketVersioningConfiguration();
        BucketVersioningConfiguration bucketVersioningConfiguration2 = new BucketVersioningConfiguration("status");
        Bucket bucketPojo = new Bucket("name");
        Owner owner = new Owner(id, "displayName");
        PartitionedPrefix partitionedPrefix = new PartitionedPrefix(PartitionDateSource.DELIVERY_TIME);
    }
}