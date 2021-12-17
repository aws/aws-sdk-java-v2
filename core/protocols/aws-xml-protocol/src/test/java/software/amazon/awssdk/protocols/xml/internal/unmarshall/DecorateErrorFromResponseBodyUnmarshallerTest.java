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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;

import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

public class DecorateErrorFromResponseBodyUnmarshallerTest {
    private static final Function<XmlElement, Optional<XmlElement>> FAIL_TEST_ERROR_ROOT_LOCATOR =
        ignored -> { throw new RuntimeException("This function should not have been called"); };

    @Test
    public void status200_noBody() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(FAIL_TEST_ERROR_ROOT_LOCATOR);

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isTrue();
        assertThat(result.parsedErrorXml()).isNull();
    }

    @Test
    public void status200_bodyWithNoError() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(FAIL_TEST_ERROR_ROOT_LOCATOR);

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .build();

        XmlElement parsedBody = XmlElement.builder()
                                          .elementName("ValidResponse")
                                          .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .parsedXml(parsedBody)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isTrue();
        assertThat(result.parsedErrorXml()).isNull();
    }

    @Test
    public void status200_bodyWithError() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(FAIL_TEST_ERROR_ROOT_LOCATOR);

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(200)
                                                                     .build();

        XmlElement parsedError = XmlElement.builder()
                                           .elementName("test-error")
                                           .build();

        XmlElement parsedBody = XmlElement.builder()
                                          .elementName("Error")
                                          .addChildElement(parsedError)
                                          .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .parsedXml(parsedBody)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isFalse();
        assertThat(result.parsedErrorXml()).isSameAs(parsedBody);
    }

    @Test
    public void status500_noBody() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(xml -> xml.getOptionalElementByName("test-error"));

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(500)
                                                                     .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isFalse();
        assertThat(result.parsedErrorXml()).isNull();
    }

    @Test
    public void status500_bodyWithNoError() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(xml -> xml.getOptionalElementByName("test-error"));

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(500)
                                                                     .build();

        XmlElement parsedBody = XmlElement.builder()
                                          .elementName("ValidResponse")
                                          .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .parsedXml(parsedBody)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isFalse();
        assertThat(result.parsedErrorXml()).isNull();
    }

    @Test
    public void status500_bodyWithError() {
        DecorateErrorFromResponseBodyUnmarshaller decorateErrorFromResponseBodyUnmarshaller =
            DecorateErrorFromResponseBodyUnmarshaller.of(xml -> xml.getOptionalElementByName("test-error"));

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpFullResponse.builder()
                                                                     .statusCode(500)
                                                                     .build();

        XmlElement parsedError = XmlElement.builder()
                                           .elementName("test-error")
                                           .build();

        XmlElement parsedBody = XmlElement.builder()
                                          .elementName("Error")
                                          .addChildElement(parsedError)
                                          .build();

        AwsXmlUnmarshallingContext context = AwsXmlUnmarshallingContext.builder()
                                                                       .sdkHttpFullResponse(sdkHttpFullResponse)
                                                                       .parsedXml(parsedBody)
                                                                       .build();

        AwsXmlUnmarshallingContext result = decorateErrorFromResponseBodyUnmarshaller.apply(context);

        assertThat(result.isResponseSuccess()).isFalse();
        assertThat(result.parsedErrorXml()).isSameAs(parsedError);
    }
}