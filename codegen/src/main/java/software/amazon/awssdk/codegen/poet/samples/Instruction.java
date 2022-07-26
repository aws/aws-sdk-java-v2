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

package software.amazon.awssdk.codegen.poet.samples;

import java.util.List;

public class Instruction {

    private String name;
    private OperationType type;
    private List<Input> input;
    private Streaming streamingInput;
    private Streaming streamingOutput;
    private OutputConsumer outputConsumer;
    private String documentation;
    private boolean useWaiter;
    private boolean usePaginator;

    public Instruction() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public List<Input> getInput() {
        return input;
    }

    public void setInput(List<Input> input) {
        this.input = input;
    }

    public Streaming getStreamingInput() {
        return streamingInput;
    }

    public void setStreamingInput(Streaming streamingInput) {
        this.streamingInput = streamingInput;
    }

    public Streaming getStreamingOutput() {
        return streamingOutput;
    }

    public void setStreamingOutput(Streaming streamingOutput) {
        this.streamingOutput = streamingOutput;
    }

    public OutputConsumer getOutputConsumer() {
        return outputConsumer;
    }

    public void setOutputConsumer(OutputConsumer outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean isUseWaiter() {
        return useWaiter;
    }

    public void setUseWaiter(boolean useWaiter) {
        this.useWaiter = useWaiter;
    }

    public boolean isUsePaginator() {
        return usePaginator;
    }

    public void setUsePaginator(boolean usePaginator) {
        this.usePaginator = usePaginator;
    }
}
