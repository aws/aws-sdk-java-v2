/*
 * Copyright 2011-2013 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.mapper.dynamodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import software.amazon.awssdk.mapper.dynamodb.test.AWSTestBase;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.transfer.TransferManager;

public class S3ClientCacheIntegrationTest extends AWSTestBase {
    private AWSCredentials credentials;

    @Before
    public void setUp() {
        credentials = new BasicAWSCredentials("mock", "mock");
    }

    @Test
    public void testClientReuse() {
        S3ClientCache s3cc = new S3ClientCache(credentials);

        TransferManager tmEast = s3cc.getTransferManager(Region.US_Standard);
        AmazonS3 s3East = s3cc.getClient(Region.US_Standard);

        assertNotNull(tmEast);
        assertNotNull(s3East);
        assertSame(s3East, tmEast.getAmazonS3Client());

        assertSame(s3East, s3cc.getClient(Region.US_Standard));
        assertSame(tmEast, s3cc.getTransferManager(Region.US_Standard));
    }

    @Test
    public void testUserProvidedClients() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3Client s3East1 = new AmazonS3Client(credentials);
        s3East1.setRegion(Region.US_Standard.toAWSRegion());
        AmazonS3Client s3West1 = new AmazonS3Client(credentials);
        s3West1.setRegion(Region.US_West.toAWSRegion());
        AmazonS3Client s3West2 = new AmazonS3Client(credentials);
        s3West2.setRegion(Region.US_West_2.toAWSRegion());

        s3cc.useClient(s3East1);
        s3cc.useClient(s3West1);
        s3cc.useClient(s3West2);

        TransferManager tmEast1 = s3cc.getTransferManager(Region.US_Standard);
        TransferManager tmWest1 = s3cc.getTransferManager(Region.US_West);
        TransferManager tmWest2 = s3cc.getTransferManager(Region.US_West_2);

        assertNotSame(tmEast1, tmWest1);
        assertNotSame(tmEast1, tmWest2);
        assertNotSame(tmWest1, tmWest2);

        assertSame(s3cc.getClient(Region.US_Standard), tmEast1.getAmazonS3Client());
        assertSame(s3cc.getClient(Region.US_West), tmWest1.getAmazonS3Client());
        assertSame(s3cc.getClient(Region.US_West_2), tmWest2.getAmazonS3Client());
    }

    @Test
    public void testS3ClientCacheWithRegionString() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion("us-east-2").withCredentials(new DefaultAWSCredentialsProviderChain()).build();
        s3cc.useClient(s3);

        TransferManager tm = s3cc.getTransferManager("us-east-2");

        assertSame(s3cc.getClient("us-east-2"), s3);
        assertSame(s3cc.getTransferManager("us-east-2"), tm);
    }

    @Test
    public void testReplaceClient() {
        S3ClientCache s3cc = new S3ClientCache(credentials);

        TransferManager tmEast1 = s3cc.getTransferManager(Region.US_Standard);
        assertNotNull(tmEast1);

        AmazonS3Client newS3East = new AmazonS3Client(credentials);
        newS3East.setRegion(Region.US_Standard.toAWSRegion());
        s3cc.useClient(newS3East); // should remove old TM

        TransferManager tmEast2 = s3cc.getTransferManager(Region.US_Standard);
        assertNotNull(tmEast2);
        assertNotSame(tmEast2, tmEast1);
        assertNotSame(tmEast2.getAmazonS3Client(), tmEast1.getAmazonS3Client());
    }

    @Test
    public void testNonExistantRegion() {
        S3ClientCache s3cc = new S3ClientCache(credentials);
        AmazonS3Client notAnAWSEndpoint = new AmazonS3Client(credentials);
        notAnAWSEndpoint.setEndpoint("s3.mordor.amazonaws.com");
        s3cc.useClient(notAnAWSEndpoint);
        assertEquals(notAnAWSEndpoint, s3cc.getClient("mordor"));
    }
}
