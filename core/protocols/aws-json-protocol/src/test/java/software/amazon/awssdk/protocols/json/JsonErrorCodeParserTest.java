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

package software.amazon.awssdk.protocols.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.json.internal.dom.JsonDomParser;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkObjectNode;
import software.amazon.awssdk.protocols.json.internal.unmarshall.JsonErrorCodeParser;
import software.amazon.awssdk.utils.StringInputStream;

public class JsonErrorCodeParserTest {

    /**
     * Value of error type present in headers for tests below
     */
    private static final String HEADER_ERROR_TYPE = "headerErrorType";

    /**
     * Value of error type present in JSON content for tests below
     */
    private static final String JSON_ERROR_TYPE = "jsonErrorType";

    private static final String ERROR_FIELD_NAME = "testErrorCode";

    private final JsonErrorCodeParser parser = new JsonErrorCodeParser(ERROR_FIELD_NAME);

    private static JsonContent toJsonContent(String errorType) throws IOException {
        SdkJsonNode node = JsonDomParser.create(new JsonFactory()).parse(new StringInputStream(
            String.format("{\"%s\": \"%s\"}", ERROR_FIELD_NAME, errorType)));
        return new JsonContent(null, node);
    }

    private static SdkHttpFullResponse httpResponseWithoutHeaders() {
        return ValidSdkObjects.sdkHttpFullResponse().build();
    }

    private static SdkHttpFullResponse httpResponseWithHeaders(String header, String value) {
        return ValidSdkObjects.sdkHttpFullResponse().putHeader(header, value).build();
    }

    @Test
    public void parseErrorType_ErrorTypeInHeadersTakesPrecedence_NoSuffix() throws IOException {
        String actualErrorType = parser.parseErrorCode(
            httpResponseWithHeaders(JsonErrorCodeParser.X_AMZN_ERROR_TYPE, HEADER_ERROR_TYPE),
            toJsonContent(JSON_ERROR_TYPE));
        assertEquals(HEADER_ERROR_TYPE, actualErrorType);
    }

    @Test
    public void parseErrorType_ErrorTypeInHeadersTakesPrecedence_SuffixIgnored() throws IOException {
        String actualErrorType = parser.parseErrorCode(
            httpResponseWithHeaders(JsonErrorCodeParser.X_AMZN_ERROR_TYPE,
                                    String.format("%s:%s", HEADER_ERROR_TYPE, "someSuffix")), toJsonContent(JSON_ERROR_TYPE));
        assertEquals(HEADER_ERROR_TYPE, actualErrorType);
    }

    @Test
    public void parseErrorType_ErrorTypeInHeaders_HonorCaseInsensitivity() throws IOException {
        String actualErrorType = parser.parseErrorCode(
            httpResponseWithHeaders("x-amzn-errortype",
                                    String.format("%s:%s", HEADER_ERROR_TYPE, "someSuffix")), toJsonContent(JSON_ERROR_TYPE));
        assertEquals(HEADER_ERROR_TYPE, actualErrorType);
    }

    @Test
    public void parseErrorType_ErrorTypeInContent_NoPrefix() throws IOException {
        String actualErrorType = parser.parseErrorCode(httpResponseWithoutHeaders(), toJsonContent(JSON_ERROR_TYPE));
        assertEquals(JSON_ERROR_TYPE, actualErrorType);
    }

    @Test
    public void parseErrorType_ErrorTypeInContent_PrefixIgnored() throws IOException {
        String actualErrorType = parser.parseErrorCode(httpResponseWithoutHeaders(),
                                                       toJsonContent(String.format("%s#%s", "somePrefix", JSON_ERROR_TYPE)));
        assertEquals(JSON_ERROR_TYPE, actualErrorType);
    }

    @Test
    public void parseErrorType_NotPresentInHeadersAndNullContent_ReturnsNull() {
        assertNull(parser.parseErrorCode(httpResponseWithoutHeaders(), null));
    }

    @Test
    public void parseErrorType_NotPresentInHeadersAndEmptyContent_ReturnsNull() {
        assertNull(parser.parseErrorCode(httpResponseWithoutHeaders(),
                                         new JsonContent(null, SdkObjectNode.emptyObject())));
    }
}
