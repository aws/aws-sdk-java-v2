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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.testutils.LogCaptor;

public class ExpiresHeaderParsingTest {

    @Test
    void test() throws IOException {

        String bucketName = "anirudkr-bucket-2";
        String key = "key1.txt";

        //File file = new RandomTempFile(10_000);
        S3Client client = S3Client.builder().build();

        //PutObjectRequest put = PutObjectRequest.builder().bucket(bucketName).key(key).expires(Instant.now()).build();
        //client.putObject(put, file.toPath());

        LogCaptor logCaptor = LogCaptor.create();

        HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucketName).key(key).build();
        Assertions.assertThatCode(() -> client.headObject(request))
                  .doesNotThrowAnyException();
        assertTrue(client.headObject(request).expires() == null);
        assertTrue(client.headObject(request).expiresString() != null);
        //assertEquals(client.headObject(request).expires().toString(), client.headObject(request).expiresString());

        List<LogEvent> events = logCaptor.loggedEvents();
        //assertLogged(events, Level.WARN, "Invalid datetime format provided in the Expires field 2034-01-01T00:00:00Z");
        String expected = "Invalid datetime format provided in the Expires field 2034-01-01T00:00:00Z";
        //assertTrue(events.stream().filter(s -> s.getMessage().getFormattedMessage().equals(expected)).collect(Collectors.toList()).isEmpty());
    }
}
