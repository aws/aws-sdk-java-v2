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

package software.amazon.awssdk.codegen.model.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.poet.samples.Sample;

public class Samples {

    private static final Samples NONE = new Samples(Collections.emptyMap());

    private Map<String, Sample> samples;

    // Needed for JSON deserialization
    private Samples() {
        this(new HashMap<>());
    }

    private Samples(Map<String, Sample> samples) {
        this.samples = samples;
    }

    public static Samples none() {
        return NONE;
    }

    public Map<String, Sample> getSamples() {
        return samples;
    }

    public void setSamples(Map<String, Sample> samples) {
        this.samples = samples;
    }

    public Sample getSample(String sampleName) {
        return samples.get(sampleName);
    }

}
