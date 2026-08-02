/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.presigner.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PresignedPostObjectRequestTest {

    @Test
    public void gettersAndBuilder_roundTrip() throws MalformedURLException {
        URL url = new URL("https://example.com/");
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("policy", "abc");

        PresignedPostObjectRequest request = PresignedPostObjectRequest.builder()
                                                                       .url(url)
                                                                       .signedFormFields(fields)
                                                                       .bucket("b")
                                                                       .key("k")
                                                                       .expiration(expiration)
                                                                       .build();

        assertThat(request.url()).isEqualTo(url);
        assertThat(request.signedFormFields()).isEqualTo(fields);
        assertThat(request.bucket()).isEqualTo("b");
        assertThat(request.key()).isEqualTo("k");
        assertThat(request.expiration()).isEqualTo(expiration);

        PresignedPostObjectRequest copy = request.toBuilder().build();
        assertThat(copy).isEqualTo(request);
        assertThat(copy.hashCode()).isEqualTo(request.hashCode());
    }
}
