/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.s3.endpoints.internal;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.DynamicEndpointAuthSchemeFactory;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class OptimizedBddEndpointProvider implements S3EndpointProvider {
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.params = params;
        RuleResult result = evaluator.evaluate();
        if (result.isEndpoint()) {
            return CompletableFuture.completedFuture(result.endpoint());
        }
        if (result.isError()) {
            String errorMsg = result.error();
            if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
            }
            return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
        }
        return CompletableFutureUtils.failedFuture(SdkClientException
                .create("Rule engine did not reach an error or endpoint result"));
    }

    private static final class Evaluator {
        S3EndpointParams params;

        String region;

        RulePartition partitionResult;

        String accessPointSuffix;

        String regionPrefix;

        String outpostId_ssa_2;

        String hardwareType;

        String _s3e_ds;

        String _s3e_fips;

        String _s3e_auth;

        RuleUrl url;

        RuleArn bucketArn;

        String s3expressAvailabilityZoneId;

        String uri_encoded_bucket;

        String arnType;

        String accessPointName_ssa_1;

        RulePartition bucketPartition;

        String outpostId_ssa_1;

        String outpostType;

        String accessPointName_ssa_2;

        public RuleResult evaluate() {
            if (cond0()) {
                if (cond1()) {
                    if (cond2()) {
                        return result0();
                    }
                    if (cond3()) {
                        if (cond4()) {
                            return result1();
                        }
                        if (cond5()) {
                            if (cond6()) {
                                return result5();
                            }
                            if (cond7()) {
                                return result5();
                            }
                            if (cond8()) {
                                if (cond9()) {
                                    if (cond10()) {
                                        if (cond11()) {
                                            if (cond12()) {
                                                if (cond13()) {
                                                    return node546();
                                                }
                                                return node507();
                                            }
                                            return node507();
                                        }
                                        return node507();
                                    }
                                    return node507();
                                }
                                return node507();
                            }
                            return node490();
                        }
                        return node479();
                    }
                    if (cond4()) {
                        return result3();
                    }
                    if (cond5()) {
                        if (cond6()) {
                            return result5();
                        }
                        if (cond7()) {
                            return result5();
                        }
                        if (cond8()) {
                            if (cond9()) {
                                if (cond10()) {
                                    if (cond11()) {
                                        if (cond12()) {
                                            if (cond13()) {
                                                return node546();
                                            }
                                            return node454();
                                        }
                                        return node454();
                                    }
                                    return node454();
                                }
                                return node454();
                            }
                            return node454();
                        }
                        if (cond14()) {
                            return node500();
                        }
                        if (cond26()) {
                            if (cond37()) {
                                if (cond38()) {
                                    return result85();
                                }
                                if (cond39()) {
                                    if (cond40()) {
                                        if (cond41()) {
                                            return result56();
                                        }
                                        if (cond42()) {
                                            return node470();
                                        }
                                        if (cond48()) {
                                            return result43();
                                        }
                                        return node499();
                                    }
                                    return result56();
                                }
                                return node464();
                            }
                            return result85();
                        }
                        return node501();
                    }
                    if (cond8()) {
                        if (cond16()) {
                            if (cond18()) {
                                if (cond19()) {
                                    if (cond22()) {
                                        return result13();
                                    }
                                    return node432();
                                }
                                return node432();
                            }
                            return node432();
                        }
                        return node432();
                    }
                    return result114();
                }
                if (cond2()) {
                    if (cond3()) {
                        if (cond4()) {
                            return result1();
                        }
                        if (cond5()) {
                            if (cond6()) {
                                return node404();
                            }
                            if (cond7()) {
                                return node394();
                            }
                            if (cond8()) {
                                if (cond9()) {
                                    if (cond10()) {
                                        if (cond11()) {
                                            if (cond12()) {
                                                if (cond13()) {
                                                    return node393();
                                                }
                                                return node364();
                                            }
                                            return node364();
                                        }
                                        return node364();
                                    }
                                    return node364();
                                }
                                return node364();
                            }
                            return node490();
                        }
                        if (cond8()) {
                            if (cond15()) {
                                return result4();
                            }
                            if (cond16()) {
                                if (cond18()) {
                                    if (cond19()) {
                                        if (cond22()) {
                                            return result13();
                                        }
                                        return node353();
                                    }
                                    return node353();
                                }
                                return node353();
                            }
                            return node353();
                        }
                        return result114();
                    }
                    if (cond4()) {
                        return result2();
                    }
                    if (cond5()) {
                        if (cond6()) {
                            return node404();
                        }
                        if (cond7()) {
                            return node394();
                        }
                        if (cond8()) {
                            if (cond9()) {
                                if (cond10()) {
                                    if (cond11()) {
                                        if (cond12()) {
                                            if (cond13()) {
                                                return node393();
                                            }
                                            return node299();
                                        }
                                        return node299();
                                    }
                                    return node299();
                                }
                                return node299();
                            }
                            return node299();
                        }
                        if (cond14()) {
                            return node500();
                        }
                        if (cond26()) {
                            if (cond37()) {
                                if (cond38()) {
                                    return result85();
                                }
                                if (cond39()) {
                                    if (cond40()) {
                                        if (cond41()) {
                                            return result56();
                                        }
                                        if (cond42()) {
                                            return node334();
                                        }
                                        return node499();
                                    }
                                    return result56();
                                }
                                return node306();
                            }
                            return result85();
                        }
                        return node501();
                    }
                    if (cond8()) {
                        if (cond15()) {
                            return result4();
                        }
                        if (cond16()) {
                            if (cond18()) {
                                if (cond19()) {
                                    if (cond22()) {
                                        return result13();
                                    }
                                    return node280();
                                }
                                return node280();
                            }
                            return node280();
                        }
                        return node280();
                    }
                    return result114();
                }
                if (cond3()) {
                    if (cond4()) {
                        return result1();
                    }
                    if (cond5()) {
                        if (cond6()) {
                            return node270();
                        }
                        if (cond7()) {
                            return node269();
                        }
                        if (cond8()) {
                            if (cond9()) {
                                if (cond10()) {
                                    if (cond11()) {
                                        if (cond12()) {
                                            if (cond13()) {
                                                return node546();
                                            }
                                            return node242();
                                        }
                                        return node242();
                                    }
                                    return node242();
                                }
                                return node242();
                            }
                            return node242();
                        }
                        return node490();
                    }
                    return node479();
                }
                if (cond4()) {
                    if (cond5()) {
                        if (cond6()) {
                            if (cond8()) {
                                if (cond16()) {
                                    if (cond18()) {
                                        if (cond19()) {
                                            if (cond20()) {
                                                if (cond21()) {
                                                    return node230();
                                                }
                                                return node414();
                                            }
                                            return node226();
                                        }
                                        return node223();
                                    }
                                    return node219();
                                }
                                return node219();
                            }
                            return node214();
                        }
                        return node101();
                    }
                    return node85();
                }
                return node6();
            }
            return result114();
        }

        private RuleResult node546() {
            if (cond17()) {
                if (cond20()) {
                    if (cond33()) {
                        if (cond44()) {
                            return result14();
                        }
                        if (cond45()) {
                            return result14();
                        }
                        return result19();
                    }
                    return node549();
                }
                return result20();
            }
            return result21();
        }

        private RuleResult node507() {
            if (cond14()) {
                if (cond26()) {
                    return result87();
                }
                return node541();
            }
            if (cond15()) {
                if (cond20()) {
                    return node539();
                }
                return node513();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result28();
                    }
                    return result29();
                }
                return result41();
            }
            return node513();
        }

        private RuleResult node490() {
            if (cond14()) {
                return node500();
            }
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node537();
                            }
                            if (cond48()) {
                                return result42();
                            }
                            return node499();
                        }
                        return result56();
                    }
                    return node517();
                }
                return result85();
            }
            return node501();
        }

        private RuleResult node479() {
            if (cond8()) {
                if (cond16()) {
                    if (cond18()) {
                        if (cond19()) {
                            if (cond22()) {
                                return result13();
                            }
                            return node484();
                        }
                        return node484();
                    }
                    return node484();
                }
                return node484();
            }
            return result114();
        }

        private RuleResult node454() {
            if (cond14()) {
                if (cond26()) {
                    return result87();
                }
                return node473();
            }
            if (cond15()) {
                if (cond20()) {
                    return node539();
                }
                return node460();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result36();
                    }
                    return result37();
                }
                return result41();
            }
            return node460();
        }

        private RuleResult node500() {
            if (cond26()) {
                return result87();
            }
            return node501();
        }

        private RuleResult node470() {
            if (cond48()) {
                return result43();
            }
            if (cond52()) {
                return result67();
            }
            return result71();
        }

        private RuleResult node499() {
            if (cond49()) {
                return result44();
            }
            return node525();
        }

        private RuleResult node464() {
            if (cond48()) {
                return result57();
            }
            if (cond50()) {
                return result74();
            }
            return result84();
        }

        private RuleResult node501() {
            if (cond28()) {
                return result86();
            }
            return result114();
        }

        private RuleResult node432() {
            if (cond35()) {
                if (cond36()) {
                    return result43();
                }
                return node434();
            }
            return result41();
        }

        private RuleResult node404() {
            if (cond8()) {
                if (cond15()) {
                    return result4();
                }
                return node406();
            }
            return result8();
        }

        private RuleResult node394() {
            if (cond8()) {
                if (cond15()) {
                    return result4();
                }
                return node396();
            }
            return result8();
        }

        private RuleResult node393() {
            if (cond15()) {
                return result4();
            }
            return node546();
        }

        private RuleResult node364() {
            if (cond14()) {
                if (cond15()) {
                    return result4();
                }
                if (cond26()) {
                    return result87();
                }
                return node390();
            }
            if (cond15()) {
                return result4();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result24();
                    }
                    return result25();
                }
                return result41();
            }
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node537();
                            }
                            if (cond48()) {
                                return result42();
                            }
                            if (cond49()) {
                                return result44();
                            }
                            if (cond51()) {
                                if (cond60()) {
                                    if (cond62()) {
                                        return result54();
                                    }
                                    return node381();
                                }
                                if (cond62()) {
                                    return result54();
                                }
                                if (cond63()) {
                                    return node381();
                                }
                                return result45();
                            }
                            return node525();
                        }
                        return result56();
                    }
                    return node517();
                }
                return result85();
            }
            return node390();
        }

        private RuleResult node353() {
            if (cond35()) {
                if (cond36()) {
                    return result42();
                }
                if (cond46()) {
                    return result103();
                }
                return result104();
            }
            return result41();
        }

        private RuleResult node299() {
            if (cond14()) {
                if (cond15()) {
                    return result4();
                }
                if (cond26()) {
                    return result87();
                }
                return node340();
            }
            if (cond15()) {
                return result4();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result26();
                    }
                    return result27();
                }
                return result41();
            }
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node334();
                            }
                            if (cond48()) {
                                if (cond49()) {
                                    return result44();
                                }
                                if (cond51()) {
                                    if (cond60()) {
                                        if (cond62()) {
                                            return result54();
                                        }
                                        return node329();
                                    }
                                    if (cond62()) {
                                        return result54();
                                    }
                                    if (cond63()) {
                                        return node329();
                                    }
                                    return result45();
                                }
                                return node525();
                            }
                            if (cond49()) {
                                return result44();
                            }
                            if (cond51()) {
                                if (cond60()) {
                                    if (cond62()) {
                                        return result54();
                                    }
                                    return node318();
                                }
                                if (cond62()) {
                                    return result54();
                                }
                                if (cond63()) {
                                    return node318();
                                }
                                return result45();
                            }
                            return node525();
                        }
                        return result56();
                    }
                    return node306();
                }
                return result85();
            }
            return node340();
        }

        private RuleResult node334() {
            if (cond48()) {
                return result55();
            }
            if (cond52()) {
                return result66();
            }
            return result71();
        }

        private RuleResult node306() {
            if (cond48()) {
                return result57();
            }
            if (cond50()) {
                return result73();
            }
            return result84();
        }

        private RuleResult node280() {
            if (cond35()) {
                if (cond36()) {
                    return result101();
                }
                if (cond46()) {
                    return result105();
                }
                return result106();
            }
            return result41();
        }

        private RuleResult node270() {
            if (cond8()) {
                return node406();
            }
            return result8();
        }

        private RuleResult node269() {
            if (cond8()) {
                return node396();
            }
            return result8();
        }

        private RuleResult node242() {
            if (cond14()) {
                if (cond26()) {
                    return result87();
                }
                return node266();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result30();
                    }
                    return result31();
                }
                return result41();
            }
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node537();
                            }
                            if (cond48()) {
                                return result42();
                            }
                            if (cond49()) {
                                return result44();
                            }
                            if (cond51()) {
                                if (cond60()) {
                                    if (cond62()) {
                                        return result54();
                                    }
                                    return node258();
                                }
                                if (cond62()) {
                                    return result54();
                                }
                                if (cond63()) {
                                    return node258();
                                }
                                return result45();
                            }
                            return node525();
                        }
                        return result56();
                    }
                    return node517();
                }
                return result85();
            }
            return node266();
        }

        private RuleResult node230() {
            if (cond30()) {
                if (cond34()) {
                    return result6();
                }
                return result7();
            }
            return result7();
        }

        private RuleResult node414() {
            if (cond27()) {
                return node418();
            }
            return result11();
        }

        private RuleResult node226() {
            if (cond21()) {
                if (cond30()) {
                    if (cond34()) {
                        return result6();
                    }
                    return result8();
                }
                return result8();
            }
            return result8();
        }

        private RuleResult node223() {
            if (cond20()) {
                if (cond21()) {
                    return result8();
                }
                return result11();
            }
            return result8();
        }

        private RuleResult node219() {
            if (cond19()) {
                if (cond20()) {
                    if (cond21()) {
                        return node230();
                    }
                    return result11();
                }
                return node226();
            }
            return node223();
        }

        private RuleResult node214() {
            if (cond19()) {
                if (cond20()) {
                    if (cond21()) {
                        return node230();
                    }
                    return result8();
                }
                return node226();
            }
            return result8();
        }

        private RuleResult node101() {
            if (cond7()) {
                if (cond8()) {
                    if (cond16()) {
                        if (cond18()) {
                            if (cond19()) {
                                if (cond20()) {
                                    if (cond21()) {
                                        return node230();
                                    }
                                    return node400();
                                }
                                return node226();
                            }
                            return node223();
                        }
                        return node219();
                    }
                    return node219();
                }
                return node214();
            }
            if (cond8()) {
                if (cond9()) {
                    if (cond10()) {
                        if (cond11()) {
                            if (cond12()) {
                                if (cond13()) {
                                    if (cond17()) {
                                        if (cond20()) {
                                            if (cond21()) {
                                                if (cond33()) {
                                                    if (cond44()) {
                                                        return result15();
                                                    }
                                                    if (cond45()) {
                                                        return result17();
                                                    }
                                                    return result19();
                                                }
                                                return node549();
                                            }
                                            return node549();
                                        }
                                        return result20();
                                    }
                                    return result21();
                                }
                                return node123();
                            }
                            return node123();
                        }
                        return node123();
                    }
                    return node123();
                }
                return node123();
            }
            if (cond14()) {
                if (cond21()) {
                    return node500();
                }
                return result22();
            }
            if (cond21()) {
                return node105();
            }
            return result22();
        }

        private RuleResult node85() {
            if (cond8()) {
                if (cond16()) {
                    if (cond18()) {
                        if (cond19()) {
                            if (cond21()) {
                                return node96();
                            }
                            if (cond22()) {
                                return result13();
                            }
                            return node94();
                        }
                        return node91();
                    }
                    return node88();
                }
                return node88();
            }
            return result114();
        }

        private RuleResult node6() {
            if (cond5()) {
                if (cond6()) {
                    return node270();
                }
                if (cond7()) {
                    return node269();
                }
                if (cond8()) {
                    if (cond9()) {
                        if (cond10()) {
                            if (cond11()) {
                                if (cond12()) {
                                    if (cond13()) {
                                        return node546();
                                    }
                                    return node23();
                                }
                                return node23();
                            }
                            return node23();
                        }
                        return node23();
                    }
                    return node23();
                }
                if (cond14()) {
                    return node500();
                }
                return node105();
            }
            if (cond8()) {
                if (cond16()) {
                    if (cond18()) {
                        if (cond19()) {
                            if (cond22()) {
                                return result13();
                            }
                            return node12();
                        }
                        return node12();
                    }
                    return node12();
                }
                return node12();
            }
            return result114();
        }

        private RuleResult node549() {
            if (cond44()) {
                return result16();
            }
            if (cond45()) {
                return result18();
            }
            return result19();
        }

        private RuleResult node541() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                return result99();
            }
            return node543();
        }

        private RuleResult node539() {
            if (cond25()) {
                return result23();
            }
            return result41();
        }

        private RuleResult node513() {
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node537();
                            }
                            if (cond48()) {
                                return result42();
                            }
                            return node523();
                        }
                        return result56();
                    }
                    return node517();
                }
                return result85();
            }
            return node541();
        }

        private RuleResult node537() {
            if (cond48()) {
                return result42();
            }
            if (cond52()) {
                return result65();
            }
            return result71();
        }

        private RuleResult node517() {
            if (cond48()) {
                return result57();
            }
            if (cond50()) {
                return result72();
            }
            return result84();
        }

        private RuleResult node484() {
            if (cond35()) {
                if (cond36()) {
                    return result42();
                }
                if (cond46()) {
                    return result107();
                }
                return result108();
            }
            return result41();
        }

        private RuleResult node473() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                return result99();
            }
            if (cond35()) {
                if (cond36()) {
                    return result43();
                }
                return result114();
            }
            return node544();
        }

        private RuleResult node460() {
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node470();
                            }
                            if (cond48()) {
                                return result43();
                            }
                            return node523();
                        }
                        return result56();
                    }
                    return node464();
                }
                return result85();
            }
            return node473();
        }

        private RuleResult node525() {
            if (cond60()) {
                return result54();
            }
            if (cond62()) {
                return result54();
            }
            if (cond63()) {
                return result54();
            }
            return result45();
        }

        private RuleResult node434() {
            if (cond46()) {
                return result111();
            }
            if (cond57()) {
                if (cond58()) {
                    return result112();
                }
                return result113();
            }
            return result113();
        }

        private RuleResult node406() {
            if (cond16()) {
                if (cond18()) {
                    if (cond19()) {
                        if (cond20()) {
                            if (cond22()) {
                                if (cond27()) {
                                    if (cond34()) {
                                        return result9();
                                    }
                                    return node418();
                                }
                                if (cond34()) {
                                    return result9();
                                }
                                return result11();
                            }
                            return node414();
                        }
                        if (cond22()) {
                            if (cond34()) {
                                return result9();
                            }
                            return result8();
                        }
                        return result8();
                    }
                    return node409();
                }
                return node409();
            }
            return node409();
        }

        private RuleResult node396() {
            if (cond16()) {
                if (cond18()) {
                    if (cond19()) {
                        if (cond20()) {
                            return node400();
                        }
                        return result8();
                    }
                    return node409();
                }
                return node409();
            }
            return node409();
        }

        private RuleResult node390() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                if (cond46()) {
                    return result88();
                }
                return result89();
            }
            return node543();
        }

        private RuleResult node381() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result59();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node340() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                if (cond46()) {
                    return result90();
                }
                return result91();
            }
            if (cond35()) {
                if (cond36()) {
                    return result101();
                }
                return result114();
            }
            return node544();
        }

        private RuleResult node329() {
            if (cond64()) {
                if (cond66()) {
                    if (cond68()) {
                        return result46();
                    }
                    if (cond70()) {
                        if (cond72()) {
                            return result48();
                        }
                        return result50();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node318() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result60();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node266() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                if (cond46()) {
                    return result92();
                }
                return result93();
            }
            return node543();
        }

        private RuleResult node258() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result61();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node418() {
            if (cond43()) {
                return result10();
            }
            if (cond47()) {
                return result10();
            }
            if (cond53()) {
                return result10();
            }
            return node421();
        }

        private RuleResult node400() {
            if (cond27()) {
                if (cond29()) {
                    return result10();
                }
                if (cond31()) {
                    return result10();
                }
                if (cond32()) {
                    return result10();
                }
                return node421();
            }
            return result11();
        }

        private RuleResult node123() {
            if (cond14()) {
                if (cond21()) {
                    if (cond26()) {
                        return result87();
                    }
                    return node196();
                }
                return result22();
            }
            if (cond20()) {
                if (cond21()) {
                    if (cond25()) {
                        if (cond30()) {
                            if (cond46()) {
                                return result32();
                            }
                            return result34();
                        }
                        if (cond46()) {
                            return result33();
                        }
                        return result35();
                    }
                    return result41();
                }
                return result22();
            }
            if (cond21()) {
                if (cond23()) {
                    if (cond24()) {
                        if (cond25()) {
                            return result35();
                        }
                        return result41();
                    }
                    return node128();
                }
                return node128();
            }
            return result22();
        }

        private RuleResult node105() {
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                if (cond48()) {
                                    return result55();
                                }
                                if (cond52()) {
                                    if (cond65()) {
                                        return result68();
                                    }
                                    return result71();
                                }
                                return result71();
                            }
                            return node499();
                        }
                        return result56();
                    }
                    if (cond48()) {
                        return result57();
                    }
                    if (cond50()) {
                        return node135();
                    }
                    return result84();
                }
                return result85();
            }
            return node501();
        }

        private RuleResult node96() {
            if (cond22()) {
                return result12();
            }
            return node97();
        }

        private RuleResult node94() {
            if (cond35()) {
                if (cond36()) {
                    return result102();
                }
                return result41();
            }
            return result41();
        }

        private RuleResult node91() {
            if (cond21()) {
                return node97();
            }
            return node94();
        }

        private RuleResult node88() {
            if (cond19()) {
                if (cond21()) {
                    return node96();
                }
                return node94();
            }
            return node91();
        }

        private RuleResult node23() {
            if (cond14()) {
                if (cond26()) {
                    return result87();
                }
                return node77();
            }
            if (cond20()) {
                if (cond25()) {
                    if (cond46()) {
                        return result38();
                    }
                    if (cond57()) {
                        if (cond58()) {
                            return result39();
                        }
                        return result40();
                    }
                    return result40();
                }
                return result41();
            }
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node184();
                            }
                            if (cond48()) {
                                if (cond49()) {
                                    return result44();
                                }
                                if (cond51()) {
                                    if (cond60()) {
                                        if (cond62()) {
                                            return result54();
                                        }
                                        return node67();
                                    }
                                    if (cond62()) {
                                        return result54();
                                    }
                                    if (cond63()) {
                                        return node67();
                                    }
                                    return result45();
                                }
                                return node525();
                            }
                            if (cond49()) {
                                return result44();
                            }
                            if (cond51()) {
                                if (cond60()) {
                                    if (cond62()) {
                                        return result54();
                                    }
                                    return node56();
                                }
                                if (cond62()) {
                                    return result54();
                                }
                                if (cond63()) {
                                    return node56();
                                }
                                return result45();
                            }
                            return node525();
                        }
                        return result56();
                    }
                    if (cond48()) {
                        return result57();
                    }
                    if (cond50()) {
                        if (cond51()) {
                            if (cond55()) {
                                return result75();
                            }
                            if (cond59()) {
                                if (cond60()) {
                                    if (cond61()) {
                                        if (cond62()) {
                                            return node40();
                                        }
                                        return node149();
                                    }
                                    return result82();
                                }
                                if (cond61()) {
                                    if (cond62()) {
                                        if (cond63()) {
                                            return node40();
                                        }
                                        return result45();
                                    }
                                    return node145();
                                }
                                return result82();
                            }
                            return result83();
                        }
                        return node135();
                    }
                    return result84();
                }
                return result85();
            }
            return node77();
        }

        private RuleResult node12() {
            if (cond35()) {
                if (cond36()) {
                    return result102();
                }
                return node434();
            }
            return result41();
        }

        private RuleResult node543() {
            if (cond35()) {
                if (cond36()) {
                    return result42();
                }
                return result114();
            }
            return node544();
        }

        private RuleResult node523() {
            if (cond49()) {
                return result44();
            }
            if (cond51()) {
                if (cond60()) {
                    if (cond62()) {
                        return result54();
                    }
                    return node532();
                }
                if (cond62()) {
                    return result54();
                }
                if (cond63()) {
                    return node532();
                }
                return result45();
            }
            return node525();
        }

        private RuleResult node544() {
            if (cond36()) {
                return result41();
            }
            return result114();
        }

        private RuleResult node409() {
            if (cond20()) {
                return result11();
            }
            return result8();
        }

        private RuleResult node421() {
            if (cond54()) {
                return result10();
            }
            if (cond56()) {
                return result10();
            }
            return result11();
        }

        private RuleResult node196() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                if (cond46()) {
                    return result94();
                }
                return result95();
            }
            if (cond35()) {
                if (cond36()) {
                    return result100();
                }
                return result114();
            }
            return node544();
        }

        private RuleResult node128() {
            if (cond26()) {
                if (cond37()) {
                    if (cond38()) {
                        return result85();
                    }
                    if (cond39()) {
                        if (cond40()) {
                            if (cond41()) {
                                return result56();
                            }
                            if (cond42()) {
                                return node184();
                            }
                            if (cond48()) {
                                if (cond49()) {
                                    return result44();
                                }
                                if (cond51()) {
                                    if (cond60()) {
                                        if (cond62()) {
                                            return result54();
                                        }
                                        return node179();
                                    }
                                    if (cond62()) {
                                        return result54();
                                    }
                                    if (cond63()) {
                                        return node179();
                                    }
                                    return result45();
                                }
                                return node525();
                            }
                            if (cond49()) {
                                return result44();
                            }
                            if (cond51()) {
                                if (cond60()) {
                                    if (cond62()) {
                                        return result54();
                                    }
                                    return node168();
                                }
                                if (cond62()) {
                                    return result54();
                                }
                                if (cond63()) {
                                    return node168();
                                }
                                return result45();
                            }
                            return node525();
                        }
                        return result56();
                    }
                    if (cond48()) {
                        return result57();
                    }
                    if (cond50()) {
                        if (cond51()) {
                            if (cond55()) {
                                return result75();
                            }
                            if (cond59()) {
                                if (cond60()) {
                                    if (cond61()) {
                                        if (cond62()) {
                                            return node152();
                                        }
                                        return node149();
                                    }
                                    return result82();
                                }
                                if (cond61()) {
                                    if (cond62()) {
                                        if (cond63()) {
                                            return node152();
                                        }
                                        return result45();
                                    }
                                    return node145();
                                }
                                return result82();
                            }
                            return result83();
                        }
                        return node135();
                    }
                    return result84();
                }
                return result85();
            }
            return node196();
        }

        private RuleResult node135() {
            if (cond55()) {
                return result75();
            }
            if (cond59()) {
                if (cond60()) {
                    return result82();
                }
                if (cond61()) {
                    if (cond63()) {
                        return result82();
                    }
                    return result45();
                }
                return result82();
            }
            return result83();
        }

        private RuleResult node97() {
            if (cond35()) {
                if (cond36()) {
                    return result100();
                }
                if (cond46()) {
                    return result109();
                }
                return result110();
            }
            return result41();
        }

        private RuleResult node77() {
            if (cond28()) {
                return result86();
            }
            if (cond34()) {
                if (cond46()) {
                    return result96();
                }
                if (cond57()) {
                    if (cond58()) {
                        return result97();
                    }
                    return result98();
                }
                return result98();
            }
            if (cond35()) {
                if (cond36()) {
                    return result102();
                }
                return result114();
            }
            return node544();
        }

        private RuleResult node184() {
            if (cond48()) {
                return result55();
            }
            if (cond52()) {
                if (cond65()) {
                    return result68();
                }
                if (cond67()) {
                    return result69();
                }
                return result70();
            }
            return result71();
        }

        private RuleResult node67() {
            if (cond64()) {
                if (cond66()) {
                    if (cond68()) {
                        return result46();
                    }
                    if (cond70()) {
                        if (cond72()) {
                            return result49();
                        }
                        return result50();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node56() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result63();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node40() {
            if (cond64()) {
                if (cond66()) {
                    if (cond70()) {
                        if (cond71()) {
                            if (cond73()) {
                                if (cond74()) {
                                    return result77();
                                }
                                return result78();
                            }
                            return result79();
                        }
                        return result80();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node149() {
            if (cond64()) {
                if (cond66()) {
                    if (cond70()) {
                        return result81();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node145() {
            if (cond63()) {
                return node149();
            }
            return result45();
        }

        private RuleResult node532() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result58();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node179() {
            if (cond64()) {
                if (cond66()) {
                    if (cond68()) {
                        return result46();
                    }
                    if (cond70()) {
                        if (cond72()) {
                            return result47();
                        }
                        return result50();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node168() {
            if (cond64()) {
                if (cond66()) {
                    if (cond69()) {
                        if (cond70()) {
                            if (cond72()) {
                                return result62();
                            }
                            return result50();
                        }
                        return result51();
                    }
                    return result64();
                }
                return result52();
            }
            return result53();
        }

        private RuleResult node152() {
            if (cond64()) {
                if (cond66()) {
                    if (cond70()) {
                        if (cond71()) {
                            if (cond73()) {
                                if (cond74()) {
                                    return result76();
                                }
                                return result78();
                            }
                            return result79();
                        }
                        return result80();
                    }
                    return result51();
                }
                return result52();
            }
            return result53();
        }

        private boolean cond0() {
            return (region != null);
        }

        private boolean cond1() {
            return (params.accelerate());
        }

        private boolean cond2() {
            return (params.useFips());
        }

        private boolean cond3() {
            return (params.useDualStack());
        }

        private boolean cond4() {
            return (params.endpoint() != null);
        }

        private boolean cond5() {
            return (params.bucket() != null);
        }

        private boolean cond6() {
            return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 0, 6, true), "")));
        }

        private boolean cond7() {
            return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 0, 7, true), "")));
        }

        private boolean cond8() {
            partitionResult = RulesFunctions.awsPartition(region);
            return partitionResult != null;
        }

        private boolean cond9() {
            accessPointSuffix = RulesFunctions.substring(params.bucket(), 0, 7, true);
            return accessPointSuffix != null;
        }

        private boolean cond10() {
            return ("--op-s3".equals(accessPointSuffix));
        }

        private boolean cond11() {
            regionPrefix = RulesFunctions.substring(params.bucket(), 8, 12, true);
            return regionPrefix != null;
        }

        private boolean cond12() {
            outpostId_ssa_2 = RulesFunctions.substring(params.bucket(), 32, 49, true);
            return outpostId_ssa_2 != null;
        }

        private boolean cond13() {
            hardwareType = RulesFunctions.substring(params.bucket(), 49, 50, true);
            return hardwareType != null;
        }

        private boolean cond14() {
            return (params.forcePathStyle());
        }

        private boolean cond15() {
            return ("aws-cn".equals(partitionResult.name()));
        }

        private boolean cond16() {
            _s3e_ds = RulesFunctions.ite(params.useDualStack(), ".dualstack", "");
            return _s3e_ds != null;
        }

        private boolean cond17() {
            return (RulesFunctions.isValidHostLabel(outpostId_ssa_2, false));
        }

        private boolean cond18() {
            _s3e_fips = RulesFunctions.ite(params.useFips(), "-fips", "");
            return _s3e_fips != null;
        }

        private boolean cond19() {
            _s3e_auth = RulesFunctions.ite(RulesFunctions.coalesce(params.disableS3ExpressSessionAuth(), false), "sigv4",
                    "sigv4-s3express");
            return _s3e_auth != null;
        }

        private boolean cond20() {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), false));
        }

        private boolean cond21() {
            url = RulesFunctions.parseURL(params.endpoint());
            return url != null;
        }

        private boolean cond22() {
            return (RulesFunctions.coalesce(params.useS3ExpressControlEndpoint(), false));
        }

        private boolean cond23() {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(params.bucket(), true));
        }

        private boolean cond24() {
            return ("http".equals(url.scheme()));
        }

        private boolean cond25() {
            return (RulesFunctions.isValidHostLabel(region, false));
        }

        private boolean cond26() {
            bucketArn = RulesFunctions.awsParseArn(params.bucket());
            return bucketArn != null;
        }

        private boolean cond27() {
            s3expressAvailabilityZoneId = RulesFunctions.listAccess(RulesFunctions.split(params.bucket(), "--", 0), -2);
            return s3expressAvailabilityZoneId != null;
        }

        private boolean cond28() {
            return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 0, 4, false), "")));
        }

        private boolean cond29() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 16, 18, true), "")));
        }

        private boolean cond30() {
            return (url.isIp());
        }

        private boolean cond31() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 21, 23, true), "")));
        }

        private boolean cond32() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 27, 29, true), "")));
        }

        private boolean cond33() {
            return ("beta".equals(regionPrefix));
        }

        private boolean cond34() {
            uri_encoded_bucket = RulesFunctions.uriEncode(params.bucket());
            return uri_encoded_bucket != null;
        }

        private boolean cond35() {
            return (RulesFunctions.isValidHostLabel(region, true));
        }

        private boolean cond36() {
            return (RulesFunctions.coalesce(params.useObjectLambdaEndpoint(), false));
        }

        private boolean cond37() {
            arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
            return arnType != null;
        }

        private boolean cond38() {
            return ("".equals(arnType));
        }

        private boolean cond39() {
            return ("accesspoint".equals(arnType));
        }

        private boolean cond40() {
            accessPointName_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
            return accessPointName_ssa_1 != null;
        }

        private boolean cond41() {
            return ("".equals(accessPointName_ssa_1));
        }

        private boolean cond42() {
            return ("".equals(bucketArn.region()));
        }

        private boolean cond43() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 14, 16, true), "")));
        }

        private boolean cond44() {
            return ("e".equals(hardwareType));
        }

        private boolean cond45() {
            return ("o".equals(hardwareType));
        }

        private boolean cond46() {
            return ("aws-global".equals(region));
        }

        private boolean cond47() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 19, 21, true), "")));
        }

        private boolean cond48() {
            return ("s3-object-lambda".equals(bucketArn.service()));
        }

        private boolean cond49() {
            return (RulesFunctions.coalesce(params.disableAccessPoints(), false));
        }

        private boolean cond50() {
            return ("s3-outposts".equals(bucketArn.service()));
        }

        private boolean cond51() {
            bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
            return bucketPartition != null;
        }

        private boolean cond52() {
            return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, true));
        }

        private boolean cond53() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 26, 28, true), "")));
        }

        private boolean cond54() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 15, 17, true), "")));
        }

        private boolean cond55() {
            return (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null);
        }

        private boolean cond56() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(params.bucket(), 20, 22, true), "")));
        }

        private boolean cond57() {
            return (params.useGlobalEndpoint());
        }

        private boolean cond58() {
            return ("us-east-1".equals(region));
        }

        private boolean cond59() {
            outpostId_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
            return outpostId_ssa_1 != null;
        }

        private boolean cond60() {
            return (RulesFunctions.coalesce(params.useArnRegion(), true));
        }

        private boolean cond61() {
            return (RulesFunctions.isValidHostLabel(outpostId_ssa_1, false));
        }

        private boolean cond62() {
            outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
            return outpostType != null;
        }

        private boolean cond63() {
            return (RulesFunctions.stringEquals(region, bucketArn.region()));
        }

        private boolean cond64() {
            return (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name()));
        }

        private boolean cond65() {
            return (params.disableMultiRegionAccessPoints());
        }

        private boolean cond66() {
            return (RulesFunctions.isValidHostLabel(bucketArn.region(), true));
        }

        private boolean cond67() {
            return (RulesFunctions.stringEquals(bucketArn.partition(), partitionResult.name()));
        }

        private boolean cond68() {
            return ("".equals(bucketArn.accountId()));
        }

        private boolean cond69() {
            return ("s3".equals(bucketArn.service()));
        }

        private boolean cond70() {
            return (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false));
        }

        private boolean cond71() {
            accessPointName_ssa_2 = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
            return accessPointName_ssa_2 != null;
        }

        private boolean cond72() {
            return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, false));
        }

        private boolean cond73() {
            return ("accesspoint".equals(outpostType));
        }

        private boolean cond74() {
            return (RulesFunctions.isValidHostLabel(accessPointName_ssa_2, false));
        }

        private RuleResult result0() {
            return RuleResult.error("Accelerate cannot be used with FIPS");
        }

        private RuleResult result1() {
            return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
        }

        private RuleResult result2() {
            return RuleResult.error("A custom endpoint cannot be combined with FIPS");
        }

        private RuleResult result3() {
            return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
        }

        private RuleResult result4() {
            return RuleResult.error("Partition does not support FIPS");
        }

        private RuleResult result5() {
            return RuleResult.error("S3Express does not support S3 Accelerate.");
        }

        private RuleResult result6() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromString(url.scheme() + "://" + url.authority() + "/" + uri_encoded_bucket
                                            + url.path()))
                            .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                            .putAttribute(
                                    AwsEndpointAttribute.AUTH_SCHEMES,
                                    Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true)
                                            .signingName("s3express").signingRegion(region).create(_s3e_auth))).build());
        }

        private RuleResult result7() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true)
                                    .signingName("s3express").signingRegion(region).create(_s3e_auth))).build());
        }

        private RuleResult result8() {
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        }

        private RuleResult result9() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3express-control" + _s3e_fips + _s3e_ds + "." + region + "."
                                    + partitionResult.dnsSuffix(), -1, "/" + uri_encoded_bucket))
                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result10() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3express" + _s3e_fips + "-"
                                    + s3expressAvailabilityZoneId + _s3e_ds + "." + region + "." + partitionResult.dnsSuffix(),
                                    -1, ""))
                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true)
                                    .signingName("s3express").signingRegion(region).create(_s3e_auth))).build());
        }

        private RuleResult result11() {
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }

        private RuleResult result12() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path()))
                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(DynamicEndpointAuthSchemeFactory.builder().disableDoubleEncoding(true)
                                    .signingName("s3express").signingRegion(region).create(_s3e_auth))).build());
        }

        private RuleResult result13() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3express-control" + _s3e_fips + _s3e_ds + "." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result14() {
            return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
        }

        private RuleResult result15() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", params.bucket() + ".ec2." + url.authority(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                    .build());
        }

        private RuleResult result16() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".ec2.s3-outposts." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                    .build());
        }

        private RuleResult result17() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".op-" + outpostId_ssa_2 + "." + url.authority(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                    .build());
        }

        private RuleResult result18() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".op-" + outpostId_ssa_2 + ".s3-outposts."
                                    + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                    .build());
        }

        private RuleResult result19() {
            return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType + "\"");
        }

        private RuleResult result20() {
            return RuleResult.error("Invalid Outposts Bucket alias - it must be a valid bucket name.");
        }

        private RuleResult result21() {
            return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
        }

        private RuleResult result22() {
            return RuleResult.error("Custom endpoint `" + params.endpoint() + "` was not a valid URI");
        }

        private RuleResult result23() {
            return RuleResult.error("S3 Accelerate cannot be used in this region");
        }

        private RuleResult result24() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips.dualstack.us-east-1."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result25() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3-fips.dualstack." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result26() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result27() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result28() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3-accelerate.dualstack.us-east-1."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result29() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result30() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result31() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3.dualstack." + region + "."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result32() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                    + params.bucket()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result33() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result34() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                    + params.bucket()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result35() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + params.bucket() + "." + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result36() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result37() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3-accelerate." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result38() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result39() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", params.bucket() + ".s3." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result40() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    params.bucket() + ".s3." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result41() {
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }

        private RuleResult result42() {
            return RuleResult.error("S3 Object Lambda does not support Dual-stack");
        }

        private RuleResult result43() {
            return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
        }

        private RuleResult result44() {
            return RuleResult.error("Access points are not supported for this operation");
        }

        private RuleResult result45() {
            return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                    + "` does not match client region `" + region + "` and UseArnRegion is `false`");
        }

        private RuleResult result46() {
            return RuleResult.error("Invalid ARN: Missing account id");
        }

        private RuleResult result47() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + "." + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result48() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                            + ".s3-object-lambda-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix(),
                                            -1, ""))
                            .putAttribute(
                                    AwsEndpointAttribute.AUTH_SCHEMES,
                                    Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                            .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result49() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + ".s3-object-lambda." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result50() {
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                    + accessPointName_ssa_1 + "`");
        }

        private RuleResult result51() {
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                    + bucketArn.accountId() + "`");
        }

        private RuleResult result52() {
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }

        private RuleResult result53() {
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`"
                    + params.bucket() + "`) has `" + bucketPartition.name() + "`");
        }

        private RuleResult result54() {
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        }

        private RuleResult result55() {
            return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
        }

        private RuleResult result56() {
            return RuleResult
                    .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        }

        private RuleResult result57() {
            return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                    + arnType + "`");
        }

        private RuleResult result58() {
            return RuleResult.error("Access Points do not support S3 Accelerate");
        }

        private RuleResult result59() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + ".s3-accesspoint-fips.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix(),
                                    -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result60() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + ".s3-accesspoint-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result61() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + ".s3-accesspoint.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1,
                                    ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result62() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + "." + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result63() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                    + ".s3-accesspoint." + bucketArn.region() + "." + bucketPartition.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(bucketArn.region()).build())).build());
        }

        private RuleResult result64() {
            return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
        }

        private RuleResult result65() {
            return RuleResult.error("S3 MRAP does not support dual-stack");
        }

        private RuleResult result66() {
            return RuleResult.error("S3 MRAP does not support FIPS");
        }

        private RuleResult result67() {
            return RuleResult.error("S3 MRAP does not support S3 Accelerate");
        }

        private RuleResult result68() {
            return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
        }

        private RuleResult result69() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_1 + ".accesspoint.s3-global."
                                    + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegionSet(Arrays.asList("*")).build())).build());
        }

        private RuleResult result70() {
            return RuleResult.error("Client was configured for partition `" + partitionResult.name()
                    + "` but bucket referred to partition `" + bucketArn.partition() + "`");
        }

        private RuleResult result71() {
            return RuleResult.error("Invalid Access Point Name");
        }

        private RuleResult result72() {
            return RuleResult.error("S3 Outposts does not support Dual-stack");
        }

        private RuleResult result73() {
            return RuleResult.error("S3 Outposts does not support FIPS");
        }

        private RuleResult result74() {
            return RuleResult.error("S3 Outposts does not support S3 Accelerate");
        }

        private RuleResult result75() {
            return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
        }

        private RuleResult result76() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_2 + "-" + bucketArn.accountId() + "."
                                    + outpostId_ssa_1 + "." + url.authority(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region())
                                    .build())).build());
        }

        private RuleResult result77() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", accessPointName_ssa_2 + "-" + bucketArn.accountId() + "."
                                    + outpostId_ssa_1 + ".s3-outposts." + bucketArn.region() + "." + bucketPartition.dnsSuffix(),
                                    -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                    .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                    .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region())
                                    .build())).build());
        }

        private RuleResult result78() {
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                    + accessPointName_ssa_2 + "`");
        }

        private RuleResult result79() {
            return RuleResult.error("Expected an outpost type `accesspoint`, found " + outpostType);
        }

        private RuleResult result80() {
            return RuleResult.error("Invalid ARN: expected an access point name");
        }

        private RuleResult result81() {
            return RuleResult.error("Invalid ARN: Expected a 4-component resource");
        }

        private RuleResult result82() {
            return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                    + outpostId_ssa_1 + "`");
        }

        private RuleResult result83() {
            return RuleResult.error("Invalid ARN: The Outpost Id was not set");
        }

        private RuleResult result84() {
            return RuleResult.error("Invalid ARN: Unrecognized format: " + params.bucket() + " (type: " + arnType + ")");
        }

        private RuleResult result85() {
            return RuleResult.error("Invalid ARN: No ARN type specified");
        }

        private RuleResult result86() {
            return RuleResult.error("Invalid ARN: `" + params.bucket() + "` was not a valid ARN");
        }

        private RuleResult result87() {
            return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
        }

        private RuleResult result88() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1,
                                    "/" + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result89() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, "/"
                                            + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result90() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, "/"
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result91() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, "/"
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result92() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, "/"
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result93() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3.dualstack." + region + "." + partitionResult.dnsSuffix(), -1,
                                    "/" + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result94() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result95() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.normalizedPath()
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result96() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "/"
                                            + uri_encoded_bucket))
                            .putAttribute(
                                    AwsEndpointAttribute.AUTH_SCHEMES,
                                    Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                            .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result97() {
            return RuleResult
                    .endpoint(Endpoint
                            .builder()
                            .endpointUrl(
                                    EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, "/"
                                            + uri_encoded_bucket))
                            .putAttribute(
                                    AwsEndpointAttribute.AUTH_SCHEMES,
                                    Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                            .signingRegion(region).build())).build());
        }

        private RuleResult result98() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3." + region + "." + partitionResult.dnsSuffix(), -1, "/"
                                    + uri_encoded_bucket))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result99() {
            return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
        }

        private RuleResult result100() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result101() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "s3-object-lambda-fips." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result102() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-object-lambda." + region + "." + partitionResult.dnsSuffix(),
                                    -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result103() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1,
                                    ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result104() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https",
                                    "s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result105() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", "s3-fips.us-east-1." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result106() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3-fips." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result107() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3.dualstack.us-east-1." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result108() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(
                            EndpointUrl.fromComponents("https", "s3.dualstack." + region + "." + partitionResult.dnsSuffix(), -1,
                                    ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result109() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result110() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromString(url.scheme() + "://" + url.authority() + url.path()))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result111() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion("us-east-1").build())).build());
        }

        private RuleResult result112() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", "s3." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result113() {
            return RuleResult.endpoint(Endpoint
                    .builder()
                    .endpointUrl(EndpointUrl.fromComponents("https", "s3." + region + "." + partitionResult.dnsSuffix(), -1, ""))
                    .putAttribute(
                            AwsEndpointAttribute.AUTH_SCHEMES,
                            Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                    .signingRegion(region).build())).build());
        }

        private RuleResult result114() {
            return RuleResult.error("A region must be set when sending requests to S3.");
        }
    }
}
