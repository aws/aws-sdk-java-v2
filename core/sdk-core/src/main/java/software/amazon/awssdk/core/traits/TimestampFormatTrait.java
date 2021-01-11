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

package software.amazon.awssdk.core.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Trait that indicates a different format should be used for marshalling/unmarshalling timestamps. If not present
 * the protocol will determine the default format to use based on the location (i.e. for JSON protocol headers are ISO8601
 * but timestamps in the payload are epoch seconds with millisecond decimal precision).
 */
@SdkProtectedApi
public final class TimestampFormatTrait implements Trait {

    private final Format format;

    private TimestampFormatTrait(Format timestampFormat) {
        this.format = timestampFormat;
    }

    /**
     * @return Format to use.
     */
    public Format format() {
        return format;
    }

    public static TimestampFormatTrait create(Format timestampFormat) {
        return new TimestampFormatTrait(timestampFormat);
    }

    /**
     * Enum of the timestamp formats we currently support.
     */
    public enum Format {

        /**
         * See {@link DateUtils#parseIso8601Date(String)}
         */
        ISO_8601,

        /**
         * See {@link DateUtils#parseRfc1123Date(String)}
         */
        RFC_822,

        /**
         * See {@link DateUtils#parseUnixTimestampInstant(String)}
         */
        UNIX_TIMESTAMP,

        /**
         * See {@link DateUtils#parseUnixTimestampMillisInstant(String)}. This is only used by the CBOR protocol currently.
         */
        UNIX_TIMESTAMP_MILLIS;

        /**
         * Creates a timestamp format enum from the string defined in the model.
         *
         * @param strFormat String format.
         * @return Format enum.
         */
        public static Format fromString(String strFormat) {
            switch (strFormat) {
                case "iso8601":
                    return ISO_8601;
                case "rfc822":
                    return RFC_822;
                case "unixTimestamp":
                    return UNIX_TIMESTAMP;
                // UNIX_TIMESTAMP_MILLIS does not have a defined string format so intentionally omitted here.
                default:
                    throw new RuntimeException("Unknown timestamp format - " + strFormat);
            }
        }
    }

}
