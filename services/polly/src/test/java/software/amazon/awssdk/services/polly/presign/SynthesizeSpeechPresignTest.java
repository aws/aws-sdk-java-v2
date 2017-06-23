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

package software.amazon.awssdk.services.polly.presign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.auth.SdkClock;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.auth.StaticSignerProvider;
import software.amazon.awssdk.auth.presign.PresignerParams;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.services.polly.model.OutputFormat;
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest;

public class SynthesizeSpeechPresignTest {

    // Note, $jacocoData is to account for the coverage tool doing some byte code manipulation.
    private static final List<String> ACKNOWLEDGED_FIELDS = Arrays.asList("lexiconNames", "outputFormat", "sampleRate",
                                                                          "text", "textType", "voiceId", "$jacocoData",
                                                                          "speechMarkTypes");

    private static final Date SIGNER_DATE = getFixedDate();

    private static final SdkClock CLOCK = new SdkClock.MockClock(SIGNER_DATE);

    private static final AwsCredentialsProvider CREDENTIALS = new StaticCredentialsProvider(
            new AwsCredentials("akid", "skid"));

    private PollyClientPresigners presigners;

    private static Date getFixedDate() {
        Calendar c = new GregorianCalendar();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 20161107T173933Z
        // Note: month is 0-based
        c.set(2016, 10, 7, 17, 39, 33);
        return c.getTime();
    }

    @Before
    public void setup() {
        presigners = new PollyClientPresigners(
                PresignerParams.builder()
                               .credentialsProvider(CREDENTIALS)
                               .endpoint(URI.create("https://polly.us-east-1.amazonaws.com"))
                               .signerProvider(createSigner())
                               .clock(CLOCK)
                               .build());
    }

    private SignerProvider createSigner() {
        final Aws4Signer signer = new Aws4Signer(CLOCK);
        signer.setOverrideDate(SIGNER_DATE);
        signer.setServiceName("polly");
        return new StaticSignerProvider(signer);
    }

    /**
     * This test is to ensure no new fields are added to {@link SynthesizeSpeechRequest} that aren't also added to {@link
     * SynthesizeSpeechPresignRequest}.
     */
    @Test
    public void allFieldsInSynthesizeSpeechRequestAreAccountedFor() {
        for (Field field : SynthesizeSpeechRequest.class.getDeclaredFields()) {
            if (!ACKNOWLEDGED_FIELDS.contains(field.getName())) {
                fail(String.format("New field, '%s', added to %s. Please ensure it's accounted for in %s." +
                                   "Once accounted for, add to the ACKNOWLEDGED_FIELDS list above",
                                   field.getName(),
                                   SynthesizeSpeechRequest.class.getSimpleName(),
                                   PollyClientPresigners.class.getSimpleName()));
            }
        }
    }

    @Test
    public void singleLexiconNameInRequest_GeneratesCorrectUrl() {
        final URL url = presigners.getPresignedSynthesizeSpeechUrl(
                new SynthesizeSpeechPresignRequest()
                        .withText("Hello world")
                        .withOutputFormat(OutputFormat.Pcm)
                        .withLexiconNames("AwsLexicon")
                        .withVoiceId("Salli"));
        assertEquals(
                "https://polly.us-east-1.amazonaws.com/v1/speech?Text=Hello%20world&VoiceId=Salli&OutputFormat=pcm&LexiconNames=AwsLexicon&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20161107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=900&X-Amz-Credential=akid%2F20161107%2Fus-east-1%2Fpolly%2Faws4_request&X-Amz-Signature=402d90c3ea6087b251dd1d7871a14d261e982ecf19ed21f7f1ad66326cbe2867",
                url.toExternalForm());
    }

    @Test
    public void multipleLexiconNamesInRequest_CanonicalizesCorrectly() {
        final URL url = presigners.getPresignedSynthesizeSpeechUrl(
                new SynthesizeSpeechPresignRequest()
                        .withExpirationDate(new DateTime(SIGNER_DATE).plusMinutes(30).toDate())
                        .withText("S3 is an AWS service")
                        .withOutputFormat(OutputFormat.Mp3)
                        .withLexiconNames("FooLexicon", "AwsLexicon")
                        .withVoiceId("Salli"));
        assertEquals(
                "https://polly.us-east-1.amazonaws.com/v1/speech?Text=S3%20is%20an%20AWS%20service&VoiceId=Salli&OutputFormat=mp3&LexiconNames=FooLexicon&LexiconNames=AwsLexicon&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20161107T173933Z&X-Amz-SignedHeaders=host&X-Amz-Expires=1800&X-Amz-Credential=akid%2F20161107%2Fus-east-1%2Fpolly%2Faws4_request&X-Amz-Signature=62f1bea76407769779e61e5b0ed18a4e40607c9637109c005f53ba1785d4874a",
                url.toExternalForm());
    }

}
