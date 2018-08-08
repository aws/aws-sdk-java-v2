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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.util.Iterator;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.utils.CompositeIterable;

/**
 * Generator tasks for AWS style clients.
 */
public class AwsGeneratorTasks implements Iterable<GeneratorTask> {
    private final Iterable<GeneratorTask> tasks;

    public AwsGeneratorTasks(GeneratorTaskParams params) {
        this.tasks = new CompositeIterable<>(new CommonGeneratorTasks(params),
                                             createAwsTasks(params));

    }

    private Iterable<GeneratorTask> createAwsTasks(GeneratorTaskParams params) {
        // TODO Move AsyncClientGeneratorTasks to common generic tasks (mostly CommonGeneratorTasks class)
        return new CompositeIterable<>(new AsyncClientGeneratorTasks(params),
                                       new PaginatorsGeneratorTasks(params),
                                       new EventStreamGeneratorTasks(params));
    }

    @Override
    public Iterator<GeneratorTask> iterator() {
        return tasks.iterator();
    }
}
