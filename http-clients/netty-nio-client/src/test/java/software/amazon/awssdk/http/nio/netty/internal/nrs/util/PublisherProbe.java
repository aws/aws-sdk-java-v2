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
 *
 * Original source licensed under the Apache License 2.0 by playframework.
 */

package software.amazon.awssdk.http.nio.netty.internal.nrs.util;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
public class PublisherProbe<T> extends Probe implements Publisher<T> {

    private final Publisher<T> publisher;

    public PublisherProbe(Publisher<T> publisher, String name) {
        super(name);
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        String sName = s == null ? "null" : s.getClass().getName();
        log("invoke subscribe with subscriber " + sName);
        publisher.subscribe(new SubscriberProbe<>(s, name, start));
        log("finish subscribe");
    }
}
