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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;

@Ignore // FIXME: Setup fails with "region cannot be null"
public class S3LinkTest {
    private DynamoDbMapper mapper;

    @Before
    public void setUp() {
        AwsCredentials credentials = new AwsCredentials("mock", "mock");
        DynamoDBClient db = DynamoDBClient.builder()
                                          .credentialsProvider(new StaticCredentialsProvider(credentials))
                                          .region(Region.US_WEST_2)
                                          .build();
        mapper = new DynamoDbMapper(db, new StaticCredentialsProvider(credentials));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullKey() {
        mapper.createS3Link("bucket", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullBucketName() {
        mapper.createS3Link(null, "key");
    }

    @Test
    public void testToJson() {
        S3Link testLink = mapper.createS3Link("bucket", "key");
        String json = testLink.toJson();

        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"bucket\",\"key\":\"key\",\"region\":\"us-east-1\"}}",
                     json);
        testLink = mapper.createS3Link("bucket", "testKey");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"bucket\",\"key\":\"testKey\",\"region\":\"us-east-1\"}}",
                     json);

        testLink = mapper.createS3Link(Region.AP_SOUTHEAST_2, "bucket", "testKey");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"bucket\",\"key\":\"testKey\",\"region\":\"ap-southeast-2\"}}",
                     json);

        testLink = mapper.createS3Link(Region.AP_SOUTHEAST_2, "test-bucket", "testKey");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"test-bucket\",\"key\":\"testKey\",\"region\":\"ap-southeast-2\"}}",
                     json);

        testLink = mapper.createS3Link(Region.AP_SOUTHEAST_2, "test-bucket", "test/key/with/slashes");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"test-bucket\",\"key\":\"test/key/with/slashes\",\"region\":\"ap-southeast-2\"}}",
                     json);

        testLink = mapper.createS3Link("test-bucket", "test/key/with/slashes");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"test-bucket\",\"key\":\"test/key/with/slashes\",\"region\":\"us-east-1\"}}",
                     json);
        testLink = mapper.createS3Link(Region.AP_SOUTHEAST_2, "test-bucket", "test/key/with/slashes");
        json = testLink.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"test-bucket\",\"key\":\"test/key/with/slashes\",\"region\":\"ap-southeast-2\"}}",
                     json);
    }

    @Test
    public void testFromJson() {
        String json = "{\"s3\":{\"region\":\"ap-southeast-2\",\"bucket\":\"test-bucket\",\"key\":\"testKey\"}}";
        S3Link s3link = S3Link.fromJson(mapper.s3ClientCache(), json);
        assertEquals("test-bucket", s3link.bucketName());
        assertEquals("ap-southeast-2", s3link.getRegion());
        assertEquals("testKey", s3link.getKey());
    }

    @Test
    public void testDefaultRegion() {
        S3Link testLink1 = mapper.createS3Link("bucket", "key");
        String json = testLink1.toJson();
        // Default to US_STANDARD if not specified
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"bucket\",\"key\":\"key\",\"region\":\"us-east-1\"}}",
                     json);
        // Default region changed to GovCloud
        testLink1 = mapper.createS3Link(Region.GovCloud.US_GOV_WEST_1, "bucket", "key");
        json = testLink1.toJson();
        assertEquals(json,
                     "{\"s3\":{\"bucket\":\"bucket\",\"key\":\"key\",\"region\":\"us-gov-west-1\"}}",
                     json);
    }

    @Test
    public void testGetRegion_ReturnsUsEast1_Whens3LinkCreated_WithNullRegion() {
        S3Link s3Link = mapper.createS3Link("bucket", "key");

        assertEquals("us-east-1", s3Link.s3Region().value());
        assertEquals("us-east-1", s3Link.getRegion());
    }

    @Test
    public void testGetRegion_ReturnsUsEast1_WhenS3LinkCreated_WithUsStandardRegion() {
        S3Link s3Link = mapper.createS3Link(Region.US_EAST_1, "bucket", "key");

        assertEquals("us-east-1", s3Link.s3Region().value());
        assertEquals("us-east-1", s3Link.getRegion());
    }

    @Test
    public void testGetRegion_ReturnsUsEast1_Whens3LinkCreated_WithUsEast1Region() {
        S3Link s3Link = mapper.createS3Link("us-east-1", "bucket", "key");

        assertEquals("us-east-1", s3Link.s3Region().value());
        assertEquals("us-east-1", s3Link.getRegion());
    }

    @Test
    public void testGetRegion_WithNonUsStandardRegion() {
        S3Link s3Link = mapper.createS3Link(Region.EU_WEST_2, "bucket", "key");

        assertEquals(Region.EU_WEST_2, s3Link.s3Region());
        assertEquals(Region.EU_WEST_2.value(), s3Link.getRegion());
    }
}