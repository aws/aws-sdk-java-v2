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

package software.amazon.awssdk.services.cloudfront.utils;

import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedCookie.getCookiesForCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedURLWithCustomPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.UTF8;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.buildCannedPolicy;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.generateResourceUrl;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.loadPrivateKey;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.makeBytesUrlSafe;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignerUtils.signWithSha1RSA;
import static software.amazon.awssdk.services.cloudfront.utils.CloudFrontSignedUrl.getSignedURLWithCannedPolicy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;

public class sandbox {

    public static void main(String[] args) throws InvalidKeySpecException, IOException, InvalidKeyException {


        /*Duration urlDuration = Duration.ofSeconds(900);
        ZonedDateTime expiry = ZonedDateTime.of(2023, 8, 27, 0, 0, 0, 0, ZoneId.of("UTC"));
        Instant expirationTime = expiry.toInstant().plus(urlDuration);
        long unixTime = expirationTime.getEpochSecond();
        long t1 = expiry.toInstant().getEpochSecond();
        long t2 = expiry.toEpochSecond();


        File keyFile = new File("./services/cloudfront/src/privateKeyCopy.pem");
        String distributionDomain = "d1npnfkc2mojrf.cloudfront.net";
        String keyId = "K2XK8WV7AVC3FY";
        //String keyName = "hello-world-html/index.html";
        String keyName = "uluruuu.jpeg";
        //String url = "https://d1npnfkc2mojrf.cloudfront.net/hello-world-html/index.html";
        String signedUrl = getSignedURLWithCannedPolicy(CloudFrontSignerUtils.Protocol.HTTPS,
                                                        distributionDomain, keyName, keyFile, keyId, expiry);
        System.out.println(signedUrl);*/
        ZonedDateTime active = ZonedDateTime.of(2040, 8, 27, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime expiry = ZonedDateTime.of(2050, 8, 27, 0, 0, 0, 0, ZoneId.of("UTC"));
        File keyFile = new File("./services/cloudfront/src/privateKeyCopy.pem");
        String domainName = "d2tizpeqz7iuy3.cloudfront.net";
        String keyId = "K3C6ZT60J2ANAT";
        String keyName = "s3ObjectKey";
        //
        String signedUrl = getSignedURLWithCustomPolicy(CloudFrontSignerUtils.Protocol.HTTPS, domainName, keyName,keyFile, keyId,
                                                        active, expiry, null);
        System.out.println(signedUrl);
        //

        String encodedPath = signedUrl.substring(signedUrl.indexOf("s3ObjectKey"));

        SdkHttpClient client = ApacheHttpClient.create();

        SdkHttpRequest sdkHttpRequest = SdkHttpRequest.builder()
                                                      .encodedPath(encodedPath)
                                                      .host(domainName)
                                                      .method(SdkHttpMethod.GET)
                                                      .protocol("https")
                                                      .build();

        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                                               .request(sdkHttpRequest).build()).call();

        System.out.println(response.httpResponse().statusCode());
        System.out.println(response.httpResponse().statusText());

        /*HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(generateResourceUrl(CloudFrontSignerUtils.Protocol.HTTPS, domainName, keyName));
        httpGet.addHeader("Cookie", cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue());
        httpGet.addHeader("Cookie", cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        httpGet.addHeader("Cookie", cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());
        HttpResponse responseCookie = httpClient.execute(httpGet);
        System.out.println("Deprac");
        System.out.println(responseCookie.getStatusLine().getStatusCode());
        System.out.println("Deprac");

        SdkHttpClient client = ApacheHttpClient.create();

        SdkHttpRequest sdkHttpRequest = SdkHttpRequest.builder()
                                               .encodedPath(encodedPath)
                                               .host(domainName)
                                               .method(SdkHttpMethod.GET)
                                               .protocol("https")
                                               .putHeader("Cookie",
                                                        cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue())
                                               .putHeader("Cookie",
                                                             cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue())
                                               .putHeader("Cookie",
                                                          cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue()).build();

        HttpExecuteResponse response = client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(sdkHttpRequest).build()).call();

        System.out.println(response.httpResponse().statusCode());
        System.out.println(response.httpResponse().statusText());

        System.out.println("Canned Cookies Encoded Path:  ");
        System.out.println(encodedPath);
        System.out.println("Cookie expires key and value: " + cookies.getExpires().getKey() + "=" + cookies.getExpires().getValue());
        System.out.println("Cookie signature kv: " + cookies.getSignature().getKey() + "=" + cookies.getSignature().getValue());
        System.out.println("Cookie keypair: " + cookies.getKeyPairId().getKey() + "=" + cookies.getKeyPairId().getValue());*/
    }
}
