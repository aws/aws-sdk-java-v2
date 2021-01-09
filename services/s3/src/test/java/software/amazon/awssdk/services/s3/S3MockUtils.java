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

package software.amazon.awssdk.services.s3;

import java.io.UnsupportedEncodingException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Various utility methods for mocking the S3Client at the HTTP layer.
 */
public final class S3MockUtils {

    private S3MockUtils() {
    }

    /**
     * @return A mocked result for the ListObjects operation.
     */
    public static HttpExecuteResponse mockListObjectsResponse() throws UnsupportedEncodingException {
        return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build()).responseBody(
            AbortableInputStream.create(new StringInputStream(
                "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n" +
                "  <Name>example-bucket</Name>\n" +
                "  <Prefix>photos/2006/</Prefix>\n" +
                "  <Marker></Marker>\n" +
                "  <MaxKeys>1000</MaxKeys>\n" +
                "  <Delimiter>/</Delimiter>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix>photos/2006/February/</Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix>photos/2006/January/</Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "</ListBucketResult>")))
                                  .build();
    }

    /**
     * @return A mocked result for the ListBuckets operation.
     */
    public static HttpExecuteResponse mockListBucketsResponse() throws UnsupportedEncodingException {
        return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build()).responseBody(
            AbortableInputStream.create(new StringInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01\">\n" +
                "  <Owner>\n" +
                "    <ID>bcaf1ffd86f461ca5fb16fd081034f</ID>\n" +
                "    <DisplayName>webfile</DisplayName>\n" +
                "  </Owner>\n" +
                "  <Buckets>\n" +
                "    <Bucket>\n" +
                "      <Name>quotes</Name>\n" +
                "      <CreationDate>2006-02-03T16:45:09.000Z</CreationDate>\n" +
                "    </Bucket>\n" +
                "  </Buckets>\n" +
                "</ListAllMyBucketsResult>")))
                                  .build();
    }
}
