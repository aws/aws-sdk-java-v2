/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.client.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.awscore.client.utils.ValidSdkObjects;
import software.amazon.awssdk.awscore.http.response.StaxResponseHandler;
import software.amazon.awssdk.awscore.protocol.xml.StaxUnmarshallerContext;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;

public class StaxResponseHandlerComponentTest {

    @Rule
    public WireMockRule wireMockServer = new WireMockRule(0);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = VerificationException.class)
    public void saxParserShouldNotExposeLocalFileSystem() throws Exception {
        File tmpFile = temporaryFolder.newFile("contents.txt");
        Files.write(tmpFile.toPath(), "hello-world".getBytes(StandardCharsets.UTF_8));

        String payload = "<?xml version=\"1.0\" ?> \n" +
                         "<!DOCTYPE a [ \n" +
                         "<!ENTITY % asd SYSTEM \"http://127.0.0.1:" + wireMockServer.port() + "/payload.dtd\"> \n" +
                         "%asd; \n" +
                         "%c; \n" +
                         "]> \n" +
                         "<a>&rrr;</a>";

        String entityString = "<!ENTITY % file SYSTEM \"file://" + tmpFile.getAbsolutePath() + "\"> \n" +
                              "<!ENTITY % c \"<!ENTITY rrr SYSTEM 'http://127.0.0.1:" + wireMockServer.port() + "/?%file;'>\">";

        stubFor(get(urlPathEqualTo("/payload.dtd")).willReturn(aResponse().withBody(entityString)));
        stubFor(get(urlPathEqualTo("/?hello-world")).willReturn(aResponse()));

        StaxResponseHandler<EmptyAwsResponse> responseHandler = new StaxResponseHandler<>(dummyUnmarshaller());

        HttpResponse response = new HttpResponse(ValidSdkObjects.sdkHttpFullRequest().build());
        response.setContent(new ByteArrayInputStream(payload.getBytes(Charset.forName("UTF-8"))));

        try {
            responseHandler.handle(response, new ExecutionAttributes());
        } catch (Exception e) {
            //expected
        }

        WireMock.verify(getRequestedFor(urlPathEqualTo("/?hello-world"))); //We expect this to fail, this call should not be made
    }

    private Unmarshaller<EmptyAwsResponse, StaxUnmarshallerContext> dummyUnmarshaller() {
        return staxContext -> {
            while(!staxContext.nextEvent().isEndDocument()) {
                //read the whole document
            }
            return EmptyAwsResponse.builder().build();
        };
    }

}