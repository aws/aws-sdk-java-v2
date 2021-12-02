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

package software.amazon.awssdk.awscore.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.DateUtils;

public class AwsServiceExceptionTest {
    private static final int SKEWED_SECONDS = 60 * 60;

    private Instant unskewedDate = Instant.now();
    private Instant futureSkewedDate = unskewedDate.plusSeconds(SKEWED_SECONDS);
    private Instant pastSkewedDate = unskewedDate.minusSeconds(SKEWED_SECONDS);

    @Test
    public void nonSkewErrorsAreIdentifiedCorrectly() {
        assertNotSkewed(0, "", 401, unskewedDate);
        assertNotSkewed(0, "", 403, unskewedDate);
        assertNotSkewed(0, "SignatureDoesNotMatch", 404, unskewedDate);
        assertNotSkewed(-SKEWED_SECONDS, "", 403, futureSkewedDate);
        assertNotSkewed(SKEWED_SECONDS, "", 403, pastSkewedDate);
        assertNotSkewed(0, "", 404, futureSkewedDate);
        assertNotSkewed(0, "", 404, pastSkewedDate);
    }

    @Test
    public void skewErrorsAreIdentifiedCorrectly() {
        assertSkewed(0, "RequestTimeTooSkewed", 404, unskewedDate);
        assertSkewed(0, "", 403, futureSkewedDate);
        assertSkewed(0, "", 401, futureSkewedDate);
        assertSkewed(0, "", 403, pastSkewedDate);
        assertSkewed(-SKEWED_SECONDS, "", 403, unskewedDate);
        assertSkewed(SKEWED_SECONDS, "", 403, unskewedDate);
    }

    public void assertSkewed(int clientSideTimeOffset,
                             String errorCode,
                             int statusCode,
                             Instant serverDate) {
        AwsServiceException exception = exception(clientSideTimeOffset, errorCode, statusCode,
                                                  DateUtils.formatRfc1123Date(serverDate));
        assertThat(exception.isClockSkewException()).isTrue();
    }

    public void assertNotSkewed(int clientSideTimeOffset,
                                String errorCode,
                                int statusCode,
                                Instant serverDate) {
        AwsServiceException exception = exception(clientSideTimeOffset, errorCode, statusCode,
                                                  DateUtils.formatRfc1123Date(serverDate));
        assertThat(exception.isClockSkewException()).isFalse();
    }

    private AwsServiceException exception(int clientSideTimeOffset, String errorCode, int statusCode, String serverDate) {
        SdkHttpResponse httpResponse =
                SdkHttpFullResponse.builder()
                                   .statusCode(statusCode)
                                   .applyMutation(r -> {
                                       if (serverDate != null) {
                                           r.putHeader("Date", serverDate);
                                       }
                                   })
                                   .build();
        AwsErrorDetails errorDetails =
                AwsErrorDetails.builder()
                               .errorCode(errorCode)
                               .sdkHttpResponse(httpResponse)
                               .build();

        return AwsServiceException.builder()
                                  .clockSkew(Duration.ofSeconds(clientSideTimeOffset))
                                  .awsErrorDetails(errorDetails)
                                  .statusCode(statusCode)
                                  .build();
    }
}