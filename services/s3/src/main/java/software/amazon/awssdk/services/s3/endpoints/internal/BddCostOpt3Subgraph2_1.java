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

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.authscheme.S3ExpressEndpointAuthScheme;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
// cost optimized round 2 bdd.  boolean and loop optimizations.  Method references  NO uriCreate optimizations
public final class BddCostOpt3Subgraph2_1 implements S3EndpointProvider {
   
    private static final ConditionFn[] CONDITION_FNS = { BddCostOpt3Subgraph2_1::cond0, BddCostOpt3Subgraph2_1::cond1,
                                                         BddCostOpt3Subgraph2_1::cond2, BddCostOpt3Subgraph2_1::cond3, BddCostOpt3Subgraph2_1::cond4,
                                                         BddCostOpt3Subgraph2_1::cond5, BddCostOpt3Subgraph2_1::cond6, BddCostOpt3Subgraph2_1::cond7,
                                                         BddCostOpt3Subgraph2_1::cond8, BddCostOpt3Subgraph2_1::cond9, BddCostOpt3Subgraph2_1::cond10,
                                                         BddCostOpt3Subgraph2_1::cond11, BddCostOpt3Subgraph2_1::cond12, BddCostOpt3Subgraph2_1::cond13,
                                                         BddCostOpt3Subgraph2_1::cond14, BddCostOpt3Subgraph2_1::cond15, BddCostOpt3Subgraph2_1::cond16,
                                                         BddCostOpt3Subgraph2_1::cond17, BddCostOpt3Subgraph2_1::cond18, BddCostOpt3Subgraph2_1::cond19,
                                                         BddCostOpt3Subgraph2_1::cond20, BddCostOpt3Subgraph2_1::cond21, BddCostOpt3Subgraph2_1::cond22,
                                                         BddCostOpt3Subgraph2_1::cond23, BddCostOpt3Subgraph2_1::cond24, BddCostOpt3Subgraph2_1::cond25,
                                                         BddCostOpt3Subgraph2_1::cond26, BddCostOpt3Subgraph2_1::cond27, BddCostOpt3Subgraph2_1::cond28,
                                                         BddCostOpt3Subgraph2_1::cond29, BddCostOpt3Subgraph2_1::cond30, BddCostOpt3Subgraph2_1::cond31,
                                                         BddCostOpt3Subgraph2_1::cond32, BddCostOpt3Subgraph2_1::cond33, BddCostOpt3Subgraph2_1::cond34,
                                                         BddCostOpt3Subgraph2_1::cond35, BddCostOpt3Subgraph2_1::cond36, BddCostOpt3Subgraph2_1::cond37,
                                                         BddCostOpt3Subgraph2_1::cond38, BddCostOpt3Subgraph2_1::cond39, BddCostOpt3Subgraph2_1::cond40,
                                                         BddCostOpt3Subgraph2_1::cond41, BddCostOpt3Subgraph2_1::cond42, BddCostOpt3Subgraph2_1::cond43,
                                                         BddCostOpt3Subgraph2_1::cond44, BddCostOpt3Subgraph2_1::cond45, BddCostOpt3Subgraph2_1::cond46,
                                                         BddCostOpt3Subgraph2_1::cond47, BddCostOpt3Subgraph2_1::cond48, BddCostOpt3Subgraph2_1::cond49,
                                                         BddCostOpt3Subgraph2_1::cond50, BddCostOpt3Subgraph2_1::cond51, BddCostOpt3Subgraph2_1::cond52,
                                                         BddCostOpt3Subgraph2_1::cond53, BddCostOpt3Subgraph2_1::cond54, BddCostOpt3Subgraph2_1::cond55,
                                                         BddCostOpt3Subgraph2_1::cond56, BddCostOpt3Subgraph2_1::cond57, BddCostOpt3Subgraph2_1::cond58,
                                                         BddCostOpt3Subgraph2_1::cond59, BddCostOpt3Subgraph2_1::cond60, BddCostOpt3Subgraph2_1::cond61,
                                                         BddCostOpt3Subgraph2_1::cond62, BddCostOpt3Subgraph2_1::cond63, BddCostOpt3Subgraph2_1::cond64,
                                                         BddCostOpt3Subgraph2_1::cond65, BddCostOpt3Subgraph2_1::cond66, BddCostOpt3Subgraph2_1::cond67,
                                                         BddCostOpt3Subgraph2_1::cond68, BddCostOpt3Subgraph2_1::cond69, BddCostOpt3Subgraph2_1::cond70,
                                                         BddCostOpt3Subgraph2_1::cond71, BddCostOpt3Subgraph2_1::cond72, BddCostOpt3Subgraph2_1::cond73,
                                                         BddCostOpt3Subgraph2_1::cond74, BddCostOpt3Subgraph2_1::cond75, BddCostOpt3Subgraph2_1::cond76,
                                                         BddCostOpt3Subgraph2_1::cond77, BddCostOpt3Subgraph2_1::cond78, BddCostOpt3Subgraph2_1::cond79,
                                                         BddCostOpt3Subgraph2_1::cond80, BddCostOpt3Subgraph2_1::cond81, BddCostOpt3Subgraph2_1::cond82,
                                                         BddCostOpt3Subgraph2_1::cond83, BddCostOpt3Subgraph2_1::cond84, BddCostOpt3Subgraph2_1::cond85,
                                                         BddCostOpt3Subgraph2_1::cond86, BddCostOpt3Subgraph2_1::cond87, BddCostOpt3Subgraph2_1::cond88,
                                                         BddCostOpt3Subgraph2_1::cond89, BddCostOpt3Subgraph2_1::cond90

    };

    private static final int[] BDD_DEFINITION = {-1,1,-1,0,3,100000200,1,578,4,2,292,5,3,23,6,4,7,100000200,5,100000206,100000207,11,9,10,12,22,10,28,11,16,29,114,16,11,13,14,12,22,14,28,15,16,29,290,16,30,17,100000128,34,21,18,88,100000197,19,89,20,100000199,90,100000198,100000199,88,100000193,100000194,34,100000100,100000101,4,53,24,5,35,25,8,30,26,9,100000009,27,10,100000009,28,21,313,29,26,34,692,9,100000009,31,10,100000009,32,21,334,33,26,34,100000200,34,326,612,8,40,36,9,100000006,37,10,100000006,38,21,313,39,26,44,692,9,100000006,41,10,100000006,42,21,334,43,26,44,100000200,34,326,45,43,46,47,47,253,47,53,248,48,54,49,100000171,55,100000171,50,56,51,256,63,260,52,64,264,627,5,187,54,8,74,55,9,147,56,10,121,57,13,58,62,14,59,62,15,60,62,16,61,62,17,100000237,62,21,65,63,22,100000238,64,26,84,66,23,701,66,27,100000234,67,28,68,692,29,69,692,30,70,695,34,355,679,34,73,72,87,100000172,117,87,100000172,120,9,147,75,10,121,76,13,77,81,14,78,81,15,79,81,16,80,81,17,100000237,81,21,100000216,82,22,100000238,83,26,84,111,34,85,714,43,86,87,47,438,87,53,433,88,54,89,100000171,55,100000171,90,56,91,441,63,445,92,64,449,93,68,94,100000143,69,100000143,95,70,100000230,813,71,97,99,72,98,99,73,99,100000132,74,100,100000140,75,101,100000139,77,102,100000151,78,103,100000138,82,100000148,100000137,33,105,100000128,34,109,106,88,100000125,107,89,108,100000127,90,100000126,100000127,88,100000117,100000118,23,100000173,111,27,116,112,28,113,100000200,29,114,100000200,30,115,100000128,34,100000129,100000188,34,120,117,88,100000182,118,89,119,100000184,90,100000183,100000184,88,100000178,100000179,19,122,123,20,135,123,22,124,100000009,34,100000222,903,36,126,127,40,100000079,127,41,128,129,42,100000083,129,46,130,131,52,100000087,131,57,132,133,58,100000091,133,61,134,100000036,62,100000095,100000036,22,136,100000009,34,100000223,915,36,138,139,40,100000059,139,41,140,141,42,100000063,141,46,142,143,52,100000067,143,57,144,145,58,100000071,145,61,146,100000036,62,100000075,100000036,11,148,149,12,153,149,19,150,151,20,152,151,22,157,100000009,22,173,100000009,19,154,155,20,170,155,22,156,171,27,186,157,34,100000213,930,39,159,160,40,168,160,44,161,162,45,100000039,162,48,163,164,49,100000047,164,50,165,166,52,100000051,166,59,167,100000036,60,100000055,100000036,44,169,100000043,45,100000039,100000043,22,172,171,27,186,100000009,27,186,173,34,100000212,944,39,175,176,40,184,176,44,177,178,45,100000018,178,48,179,180,49,100000026,180,50,181,182,52,100000030,182,59,183,100000036,60,100000034,100000036,44,185,100000022,45,100000018,100000022,34,100000014,100000015,6,211,188,8,198,189,9,100000006,190,10,100000006,191,13,192,196,14,193,196,15,194,196,16,195,196,17,100000237,196,21,222,197,22,207,221,9,100000006,199,10,100000006,200,13,201,205,14,202,205,15,203,205,16,204,205,17,100000237,205,21,100000236,206,22,207,238,33,208,100000128,34,210,209,88,100000123,100000124,88,100000115,100000116,8,229,212,9,100000006,213,10,100000006,214,13,215,219,14,216,219,15,217,219,16,218,219,17,100000237,219,21,222,220,22,285,221,26,239,223,23,701,223,27,681,224,28,225,692,29,226,692,30,227,695,34,355,228,87,100000172,100000130,9,100000006,230,10,100000006,231,13,232,236,14,233,236,15,234,236,16,235,236,17,100000237,236,21,100000236,237,22,285,238,26,239,287,34,266,240,43,241,242,47,253,242,53,248,243,54,244,100000171,55,100000171,245,56,246,256,63,260,247,64,264,274,54,249,100000171,55,100000171,250,56,251,256,63,260,252,64,264,828,54,254,100000171,55,100000171,255,56,258,256,64,100000144,257,65,100000161,100000170,63,260,259,64,264,857,64,264,261,68,262,100000143,69,100000143,263,84,100000154,100000158,68,265,100000143,69,100000143,100000130,43,267,268,47,438,268,53,433,269,54,270,100000171,55,100000171,271,56,272,441,63,445,273,64,449,274,68,275,100000143,69,100000143,276,70,100000226,813,71,278,280,72,279,280,73,280,100000132,74,281,100000140,75,282,100000139,77,283,100000151,78,284,100000138,82,100000145,100000137,33,100000110,100000128,23,100000173,287,27,100000185,288,28,289,100000200,29,290,100000200,30,291,100000128,34,100000129,100000130,3,306,100000203,4,295,294,5,100000001,100000200,5,100000001,296,6,100000005,297,11,298,299,12,305,299,28,300,301,29,459,301,30,302,100000128,34,304,303,88,100000191,100000192,88,100000189,100000190,34,100000098,100000099,4,335,307,5,100000001,308,8,314,309,9,100000009,310,10,100000009,311,21,313,312,26,318,692,23,701,692,9,100000009,315,10,100000009,316,21,334,317,26,318,100000200,34,326,319,43,320,321,47,404,321,53,400,322,54,323,100000171,55,100000171,324,56,325,407,63,410,627,43,327,328,47,438,328,53,433,329,54,330,100000171,55,100000171,331,56,332,441,63,445,333,64,449,627,23,100000173,100000200,5,100000001,336,6,100000005,337,8,359,338,9,514,339,10,468,340,13,341,345,14,342,345,15,343,345,16,344,345,17,100000237,345,21,348,346,22,451,347,26,369,349,23,701,349,27,356,350,28,351,692,29,352,692,30,353,695,34,355,354,87,100000172,100000187,87,100000172,100000129,34,358,357,87,100000172,462,87,100000172,463,9,514,360,10,468,361,13,362,366,14,363,366,15,364,366,16,365,366,17,100000237,366,21,100000224,367,22,451,368,26,369,456,34,414,370,43,371,372,47,404,372,53,400,373,54,374,100000171,55,100000171,375,56,376,407,63,410,377,64,389,378,68,379,100000143,69,100000143,380,70,100000228,813,71,382,384,72,383,384,73,384,100000132,74,385,100000140,75,386,100000139,77,387,100000151,78,388,100000138,82,100000147,100000137,68,390,100000143,69,100000143,391,70,100000227,813,71,393,395,72,394,395,73,395,100000132,74,396,100000140,75,397,100000139,76,100000133,398,78,399,100000138,82,100000135,100000137,54,401,100000171,55,100000171,402,56,403,407,63,410,828,54,405,100000171,55,100000171,406,56,409,407,64,100000144,408,65,100000160,100000170,63,410,857,64,866,411,68,412,100000143,69,100000143,413,84,100000153,100000158,43,415,416,47,438,416,53,433,417,54,418,100000171,55,100000171,419,56,420,441,63,445,421,64,449,422,68,423,100000143,69,100000143,424,70,100000229,813,71,426,428,72,427,428,73,428,100000132,74,429,100000140,75,430,100000139,77,431,100000151,78,432,100000138,82,100000146,100000137,54,434,100000171,55,100000171,435,56,436,441,63,445,437,64,449,828,54,439,100000171,55,100000171,440,56,443,441,64,100000144,442,65,100000159,100000170,63,445,444,64,449,857,64,449,446,68,447,100000143,69,100000143,448,84,100000152,100000158,68,450,100000143,69,100000143,100000129,33,452,100000128,34,454,453,88,100000113,100000114,88,100000111,100000112,23,100000173,456,27,461,457,28,458,100000200,29,459,100000200,30,460,100000128,34,100000129,100000187,34,463,462,88,100000176,100000177,88,100000174,100000175,31,465,100000108,32,466,893,37,100000102,467,38,100000102,100000107,19,469,470,20,492,470,22,471,100000009,34,100000219,100000218,36,473,474,40,100000078,474,41,475,476,42,100000082,476,46,477,478,52,100000086,478,57,479,480,58,100000090,480,61,481,100000036,62,100000094,100000036,36,483,484,40,100000077,484,41,485,486,42,100000081,486,46,487,488,52,100000085,488,57,489,490,58,100000089,490,61,491,100000036,62,100000093,100000036,22,493,100000009,34,100000221,100000220,36,495,496,40,100000058,496,41,497,498,42,100000062,498,46,499,500,52,100000066,500,57,501,502,58,100000070,502,61,503,100000036,62,100000074,100000036,36,505,506,40,100000057,506,41,507,508,42,100000061,508,46,509,510,52,100000065,510,57,511,512,58,100000069,512,61,513,100000036,62,100000073,100000036,11,515,516,12,520,516,19,517,518,20,519,518,22,524,100000009,22,552,100000009,19,521,522,20,549,522,22,523,550,27,577,524,34,100000210,100000211,39,526,527,40,535,527,44,528,529,45,100000038,529,48,530,531,49,100000046,531,50,532,533,52,100000050,533,59,534,100000036,60,100000054,100000036,44,536,100000042,45,100000038,100000042,39,538,539,40,547,539,44,540,541,45,100000037,541,48,542,543,49,100000045,543,50,544,545,52,100000049,545,59,546,100000036,60,100000053,100000036,44,548,100000041,45,100000037,100000041,22,551,550,27,577,100000009,27,577,552,34,100000208,100000209,39,554,555,40,563,555,44,556,557,45,100000017,557,48,558,559,49,100000025,559,50,560,561,52,100000029,561,59,562,100000036,60,100000033,100000036,44,564,100000021,45,100000017,100000021,39,566,567,40,575,567,44,568,569,45,100000016,569,48,570,571,49,100000024,571,50,572,573,52,100000028,573,59,574,100000036,60,100000032,100000036,44,576,100000020,45,100000016,100000020,34,100000012,100000013,2,976,579,3,596,580,4,582,581,5,975,880,5,975,583,11,584,585,12,593,585,18,588,586,28,587,882,29,774,882,28,589,590,29,881,590,30,591,882,34,100000002,592,88,100000195,100000196,18,595,594,34,100000002,100000101,34,100000002,100000097,4,651,597,5,975,598,7,601,599,9,650,600,10,650,660,8,607,602,9,650,603,10,650,604,21,606,605,26,611,691,23,700,691,9,650,608,10,650,609,21,649,610,26,611,880,34,100000002,612,43,613,614,47,633,614,51,622,615,53,619,616,54,617,100000171,55,100000171,618,56,626,100000235,54,620,100000171,55,100000171,621,56,632,100000235,53,629,623,54,624,100000171,55,100000171,625,56,626,854,63,645,627,68,628,100000143,69,100000143,813,54,630,100000171,55,100000171,631,56,632,854,63,645,828,51,641,634,54,635,100000171,55,100000171,636,56,644,100000235,64,100000144,638,65,639,100000170,66,640,100000169,67,839,100000168,54,642,100000171,55,100000171,643,56,644,854,63,645,857,64,866,646,68,647,100000143,69,100000143,648,83,865,100000158,23,886,880,18,956,968,5,975,652,7,661,653,9,925,654,10,898,655,13,656,660,14,657,660,15,658,660,16,659,660,17,100000214,660,34,100000002,100000109,8,702,662,9,925,663,10,898,664,13,665,669,14,666,669,15,667,669,16,668,669,17,100000214,669,18,682,670,21,673,671,22,882,672,26,713,674,23,700,674,27,680,675,28,676,691,29,677,691,30,678,694,34,100000002,679,87,100000172,100000188,34,100000002,681,87,100000172,100000185,21,687,683,22,870,684,24,685,686,25,868,686,26,782,688,23,700,688,27,698,689,28,690,691,29,693,691,34,100000002,692,87,100000172,100000200,30,696,694,34,100000002,695,87,100000172,100000128,34,100000002,697,87,100000172,100000186,34,100000002,699,87,100000172,885,34,100000002,701,87,100000172,100000173,9,925,703,10,898,704,13,705,709,14,706,709,15,707,709,16,708,709,17,100000214,709,18,777,710,21,770,711,22,882,712,26,713,771,34,100000002,714,43,715,716,47,752,716,51,724,717,53,721,718,54,719,100000171,55,100000171,720,56,728,756,54,722,100000171,55,100000171,723,56,827,756,53,824,725,54,726,100000171,55,100000171,727,56,728,854,63,859,729,64,741,730,68,731,100000143,69,100000143,732,70,100000232,813,71,734,736,72,735,736,73,736,100000132,74,737,100000140,75,738,100000139,77,739,100000151,78,740,100000138,82,100000150,100000137,68,742,100000143,69,100000143,743,70,100000231,813,71,745,747,72,746,747,73,747,100000132,74,748,100000140,75,749,100000139,76,100000133,750,78,751,100000138,82,100000136,100000137,51,851,753,54,754,100000171,55,100000171,755,56,856,756,64,100000144,100000201,65,758,100000170,66,759,100000169,67,760,100000168,70,761,839,71,762,764,72,763,764,73,764,100000132,74,765,100000140,75,766,100000139,78,767,100000138,79,768,100000167,80,769,100000166,81,100000164,100000165,23,886,771,27,776,772,28,773,880,29,774,880,30,775,882,34,100000002,100000188,34,100000002,100000185,21,876,778,22,870,779,24,780,781,25,868,781,26,782,877,34,100000002,783,43,784,785,47,830,785,51,793,786,53,790,787,54,788,100000171,55,100000171,789,56,797,834,54,791,100000171,55,100000171,792,56,827,834,53,824,794,54,795,100000171,55,100000171,796,56,797,854,63,859,798,64,810,799,68,800,100000143,69,100000143,801,70,100000225,813,71,803,805,72,804,805,73,805,100000132,74,806,100000140,75,807,100000139,77,808,100000151,78,809,100000138,82,100000149,100000137,68,811,100000143,69,100000143,812,70,100000233,813,71,814,100000141,72,815,100000141,73,100000141,100000132,71,817,819,72,818,819,73,819,100000132,74,820,100000140,75,821,100000139,76,100000133,822,78,823,100000138,82,100000134,100000137,54,825,100000171,55,100000171,826,56,827,854,63,859,828,68,829,100000143,69,100000143,100000141,51,851,831,54,832,100000171,55,100000171,833,56,856,834,64,100000144,100000202,65,836,100000170,66,837,100000169,67,838,100000168,70,842,839,71,840,100000168,72,841,100000168,73,100000168,100000132,71,843,845,72,844,845,73,845,100000132,74,846,100000140,75,847,100000139,78,848,100000138,79,849,100000167,80,850,100000166,81,100000163,100000165,54,852,100000171,55,100000171,853,56,856,854,64,100000144,855,65,100000162,100000170,63,859,857,68,858,100000143,69,100000143,100000131,64,866,100000239,68,861,100000143,69,100000143,862,83,865,863,84,864,100000158,85,100000156,100000157,84,100000155,100000158,68,867,100000143,69,100000143,100000142,33,869,882,34,100000002,100000122,33,871,882,34,100000002,872,35,875,873,86,874,100000128,88,100000120,100000122,88,100000119,100000121,23,886,877,27,884,878,28,879,880,29,881,880,34,100000002,100000200,30,883,882,34,100000002,100000128,34,100000002,100000186,34,100000002,885,88,100000180,100000181,34,100000002,100000173,18,889,888,31,892,890,31,891,890,34,100000002,100000108,32,895,892,34,100000002,893,37,100000104,894,38,100000106,100000107,34,100000002,896,37,100000103,897,38,100000105,100000107,18,956,899,19,900,901,20,913,901,22,100000217,968,34,100000002,903,36,904,905,40,100000080,905,41,906,907,42,100000084,907,46,908,909,52,100000088,909,57,910,911,58,100000092,911,61,912,100000036,62,100000096,100000036,22,100000215,968,34,100000002,915,36,916,917,40,100000060,917,41,918,919,42,100000064,919,46,920,921,52,100000068,921,57,922,923,58,100000072,923,61,924,100000036,62,100000076,100000036,18,956,926,19,927,928,20,942,928,22,100000204,968,34,100000002,930,39,931,932,40,940,932,44,933,934,45,100000040,934,48,935,936,49,100000048,936,50,937,938,52,100000052,938,59,939,100000036,60,100000056,100000036,44,941,100000044,45,100000040,100000044,22,100000205,968,34,100000002,944,39,945,946,40,954,946,44,947,948,45,100000019,948,48,949,950,49,100000027,950,50,951,952,52,100000031,952,59,953,100000036,60,100000035,100000036,44,955,100000023,45,100000019,100000023,19,957,958,20,966,958,22,962,959,27,960,968,34,100000002,961,35,100000010,100000009,27,964,963,34,100000002,100000011,34,100000002,965,35,100000010,100000011,22,971,967,27,969,968,34,100000002,100000009,34,100000002,970,35,100000007,100000009,27,973,972,34,100000002,100000008,34,100000002,974,35,100000007,100000008,34,100000002,100000004,5,100000001,977,34,100000002,100000003};
    private static final ResultFn[] RESULT_FNS = {BddCostOpt3Subgraph2_1::result0,BddCostOpt3Subgraph2_1::result1,BddCostOpt3Subgraph2_1::result2,BddCostOpt3Subgraph2_1::result3,BddCostOpt3Subgraph2_1::result4,BddCostOpt3Subgraph2_1::result5,BddCostOpt3Subgraph2_1::result6,BddCostOpt3Subgraph2_1::result7,BddCostOpt3Subgraph2_1::result8,BddCostOpt3Subgraph2_1::result9,BddCostOpt3Subgraph2_1::result10,BddCostOpt3Subgraph2_1::result11,BddCostOpt3Subgraph2_1::result12,BddCostOpt3Subgraph2_1::result13,BddCostOpt3Subgraph2_1::result14,BddCostOpt3Subgraph2_1::result15,BddCostOpt3Subgraph2_1::result16,BddCostOpt3Subgraph2_1::result17,BddCostOpt3Subgraph2_1::result18,BddCostOpt3Subgraph2_1::result19,BddCostOpt3Subgraph2_1::result20,BddCostOpt3Subgraph2_1::result21,BddCostOpt3Subgraph2_1::result22,BddCostOpt3Subgraph2_1::result23,BddCostOpt3Subgraph2_1::result24,BddCostOpt3Subgraph2_1::result25,BddCostOpt3Subgraph2_1::result26,BddCostOpt3Subgraph2_1::result27,BddCostOpt3Subgraph2_1::result28,BddCostOpt3Subgraph2_1::result29,BddCostOpt3Subgraph2_1::result30,BddCostOpt3Subgraph2_1::result31,BddCostOpt3Subgraph2_1::result32,BddCostOpt3Subgraph2_1::result33,BddCostOpt3Subgraph2_1::result34,BddCostOpt3Subgraph2_1::result35,BddCostOpt3Subgraph2_1::result36,BddCostOpt3Subgraph2_1::result37,BddCostOpt3Subgraph2_1::result38,BddCostOpt3Subgraph2_1::result39,BddCostOpt3Subgraph2_1::result40,BddCostOpt3Subgraph2_1::result41,BddCostOpt3Subgraph2_1::result42,BddCostOpt3Subgraph2_1::result43,BddCostOpt3Subgraph2_1::result44,BddCostOpt3Subgraph2_1::result45,BddCostOpt3Subgraph2_1::result46,BddCostOpt3Subgraph2_1::result47,BddCostOpt3Subgraph2_1::result48,BddCostOpt3Subgraph2_1::result49,BddCostOpt3Subgraph2_1::result50,BddCostOpt3Subgraph2_1::result51,BddCostOpt3Subgraph2_1::result52,BddCostOpt3Subgraph2_1::result53,BddCostOpt3Subgraph2_1::result54,BddCostOpt3Subgraph2_1::result55,BddCostOpt3Subgraph2_1::result56,BddCostOpt3Subgraph2_1::result57,BddCostOpt3Subgraph2_1::result58,BddCostOpt3Subgraph2_1::result59,BddCostOpt3Subgraph2_1::result60,BddCostOpt3Subgraph2_1::result61,BddCostOpt3Subgraph2_1::result62,BddCostOpt3Subgraph2_1::result63,BddCostOpt3Subgraph2_1::result64,BddCostOpt3Subgraph2_1::result65,BddCostOpt3Subgraph2_1::result66,BddCostOpt3Subgraph2_1::result67,BddCostOpt3Subgraph2_1::result68,BddCostOpt3Subgraph2_1::result69,BddCostOpt3Subgraph2_1::result70,BddCostOpt3Subgraph2_1::result71,BddCostOpt3Subgraph2_1::result72,BddCostOpt3Subgraph2_1::result73,BddCostOpt3Subgraph2_1::result74,BddCostOpt3Subgraph2_1::result75,BddCostOpt3Subgraph2_1::result76,BddCostOpt3Subgraph2_1::result77,BddCostOpt3Subgraph2_1::result78,BddCostOpt3Subgraph2_1::result79,BddCostOpt3Subgraph2_1::result80,BddCostOpt3Subgraph2_1::result81,BddCostOpt3Subgraph2_1::result82,BddCostOpt3Subgraph2_1::result83,BddCostOpt3Subgraph2_1::result84,BddCostOpt3Subgraph2_1::result85,BddCostOpt3Subgraph2_1::result86,BddCostOpt3Subgraph2_1::result87,BddCostOpt3Subgraph2_1::result88,BddCostOpt3Subgraph2_1::result89,BddCostOpt3Subgraph2_1::result90,BddCostOpt3Subgraph2_1::result91,BddCostOpt3Subgraph2_1::result92,BddCostOpt3Subgraph2_1::result93,BddCostOpt3Subgraph2_1::result94,BddCostOpt3Subgraph2_1::result95,BddCostOpt3Subgraph2_1::result96,BddCostOpt3Subgraph2_1::result97,BddCostOpt3Subgraph2_1::result98,BddCostOpt3Subgraph2_1::result99,BddCostOpt3Subgraph2_1::result100,BddCostOpt3Subgraph2_1::result101,BddCostOpt3Subgraph2_1::result102,BddCostOpt3Subgraph2_1::result103,BddCostOpt3Subgraph2_1::result104,BddCostOpt3Subgraph2_1::result105,BddCostOpt3Subgraph2_1::result106,BddCostOpt3Subgraph2_1::result107,BddCostOpt3Subgraph2_1::result108,BddCostOpt3Subgraph2_1::result109,BddCostOpt3Subgraph2_1::result110,BddCostOpt3Subgraph2_1::result111,BddCostOpt3Subgraph2_1::result112,BddCostOpt3Subgraph2_1::result113,BddCostOpt3Subgraph2_1::result114,BddCostOpt3Subgraph2_1::result115,BddCostOpt3Subgraph2_1::result116,BddCostOpt3Subgraph2_1::result117,BddCostOpt3Subgraph2_1::result118,BddCostOpt3Subgraph2_1::result119,BddCostOpt3Subgraph2_1::result120,BddCostOpt3Subgraph2_1::result121,BddCostOpt3Subgraph2_1::result122,BddCostOpt3Subgraph2_1::result123,BddCostOpt3Subgraph2_1::result124,BddCostOpt3Subgraph2_1::result125,BddCostOpt3Subgraph2_1::result126,BddCostOpt3Subgraph2_1::result127,BddCostOpt3Subgraph2_1::result128,BddCostOpt3Subgraph2_1::result129,BddCostOpt3Subgraph2_1::result130,BddCostOpt3Subgraph2_1::result131,BddCostOpt3Subgraph2_1::result132,BddCostOpt3Subgraph2_1::result133,BddCostOpt3Subgraph2_1::result134,BddCostOpt3Subgraph2_1::result135,BddCostOpt3Subgraph2_1::result136,BddCostOpt3Subgraph2_1::result137,BddCostOpt3Subgraph2_1::result138,BddCostOpt3Subgraph2_1::result139,BddCostOpt3Subgraph2_1::result140,BddCostOpt3Subgraph2_1::result141,BddCostOpt3Subgraph2_1::result142,BddCostOpt3Subgraph2_1::result143,BddCostOpt3Subgraph2_1::result144,BddCostOpt3Subgraph2_1::result145,BddCostOpt3Subgraph2_1::result146,BddCostOpt3Subgraph2_1::result147,BddCostOpt3Subgraph2_1::result148,BddCostOpt3Subgraph2_1::result149,BddCostOpt3Subgraph2_1::result150,BddCostOpt3Subgraph2_1::result151,BddCostOpt3Subgraph2_1::result152,BddCostOpt3Subgraph2_1::result153,BddCostOpt3Subgraph2_1::result154,BddCostOpt3Subgraph2_1::result155,BddCostOpt3Subgraph2_1::result156,BddCostOpt3Subgraph2_1::result157,BddCostOpt3Subgraph2_1::result158,BddCostOpt3Subgraph2_1::result159,BddCostOpt3Subgraph2_1::result160,BddCostOpt3Subgraph2_1::result161,BddCostOpt3Subgraph2_1::result162,BddCostOpt3Subgraph2_1::result163,BddCostOpt3Subgraph2_1::result164,BddCostOpt3Subgraph2_1::result165,BddCostOpt3Subgraph2_1::result166,BddCostOpt3Subgraph2_1::result167,BddCostOpt3Subgraph2_1::result168,BddCostOpt3Subgraph2_1::result169,BddCostOpt3Subgraph2_1::result170,BddCostOpt3Subgraph2_1::result171,BddCostOpt3Subgraph2_1::result172,BddCostOpt3Subgraph2_1::result173,BddCostOpt3Subgraph2_1::result174,BddCostOpt3Subgraph2_1::result175,BddCostOpt3Subgraph2_1::result176,BddCostOpt3Subgraph2_1::result177,BddCostOpt3Subgraph2_1::result178,BddCostOpt3Subgraph2_1::result179,BddCostOpt3Subgraph2_1::result180,BddCostOpt3Subgraph2_1::result181,BddCostOpt3Subgraph2_1::result182,BddCostOpt3Subgraph2_1::result183,BddCostOpt3Subgraph2_1::result184,BddCostOpt3Subgraph2_1::result185,BddCostOpt3Subgraph2_1::result186,BddCostOpt3Subgraph2_1::result187,BddCostOpt3Subgraph2_1::result188,BddCostOpt3Subgraph2_1::result189,BddCostOpt3Subgraph2_1::result190,BddCostOpt3Subgraph2_1::result191,BddCostOpt3Subgraph2_1::result192,BddCostOpt3Subgraph2_1::result193,BddCostOpt3Subgraph2_1::result194,BddCostOpt3Subgraph2_1::result195,BddCostOpt3Subgraph2_1::result196,BddCostOpt3Subgraph2_1::result197,BddCostOpt3Subgraph2_1::result198,BddCostOpt3Subgraph2_1::result199,BddCostOpt3Subgraph2_1::result200,BddCostOpt3Subgraph2_1::result201,BddCostOpt3Subgraph2_1::result202,BddCostOpt3Subgraph2_1::result203,BddCostOpt3Subgraph2_1::result204,BddCostOpt3Subgraph2_1::result205,BddCostOpt3Subgraph2_1::result206,BddCostOpt3Subgraph2_1::result207,BddCostOpt3Subgraph2_1::result208,BddCostOpt3Subgraph2_1::result209,BddCostOpt3Subgraph2_1::result210,BddCostOpt3Subgraph2_1::result211,BddCostOpt3Subgraph2_1::result212,BddCostOpt3Subgraph2_1::result213,BddCostOpt3Subgraph2_1::result214,BddCostOpt3Subgraph2_1::result215,BddCostOpt3Subgraph2_1::result216,BddCostOpt3Subgraph2_1::result217,BddCostOpt3Subgraph2_1::result218,BddCostOpt3Subgraph2_1::result219,BddCostOpt3Subgraph2_1::result220,BddCostOpt3Subgraph2_1::result221,BddCostOpt3Subgraph2_1::result222,BddCostOpt3Subgraph2_1::result223,BddCostOpt3Subgraph2_1::result224,BddCostOpt3Subgraph2_1::result225,BddCostOpt3Subgraph2_1::result226,BddCostOpt3Subgraph2_1::result227,BddCostOpt3Subgraph2_1::result228,BddCostOpt3Subgraph2_1::result229,BddCostOpt3Subgraph2_1::result230,BddCostOpt3Subgraph2_1::result231,BddCostOpt3Subgraph2_1::result232,BddCostOpt3Subgraph2_1::result233,BddCostOpt3Subgraph2_1::result234,BddCostOpt3Subgraph2_1::result235,BddCostOpt3Subgraph2_1::result236,BddCostOpt3Subgraph2_1::result237,BddCostOpt3Subgraph2_1::result238};
    private static RuleResult result200(Registers registers) {
        // this is a subgraph that replaces node 756 and its subnodes: #<Set: {757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 838, 839, 840}>
        if ("s3-outposts".equals(registers.bucketArn.service())) { // node 756
            registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
            if (registers.outpostId_ssa_1 != null) { // node 757
                if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false)) { // node 758
                    registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
                    if (registers.bucketPartition != null) { // node 759
                        if (registers.useArnRegion != null) { // node 760
                            if (!registers.useArnRegion) { // node 761
                                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 762
                                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 763
                                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 764
                                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 765
                                                registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                                if (registers.outpostType != null) { // node 766
                                                    registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                                    if (registers.accessPointName_ssa_2 != null) { // node 767
                                                        if ("accesspoint".equals(registers.outpostType)) { // node 768
                                                            return result163(registers);
                                                        }
                                                        return result164(registers);
                                                    }
                                                    return result165(registers);
                                                }
                                                return result166(registers);
                                            }
                                            return result137(registers);
                                        }
                                        return result138(registers);
                                    }
                                    return result139(registers);
                                }
                                return result131(registers);
                            }
                            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 763
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 764
                                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 765
                                        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                        if (registers.outpostType != null) { // node 766
                                            registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                            if (registers.accessPointName_ssa_2 != null) { // node 767
                                                if ("accesspoint".equals(registers.outpostType)) { // node 768
                                                    return result163(registers);
                                                }
                                                return result164(registers);
                                            }
                                            return result165(registers);
                                        }
                                        return result166(registers);
                                    }
                                    return result137(registers);
                                }
                                return result138(registers);
                            }
                            return result139(registers);
                        }
                        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 763
                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 764
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 765
                                    registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                    if (registers.outpostType != null) { // node 766
                                        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                        if (registers.accessPointName_ssa_2 != null) { // node 767
                                            if ("accesspoint".equals(registers.outpostType)) { // node 768
                                                return result163(registers);
                                            }
                                            return result164(registers);
                                        }
                                        return result165(registers);
                                    }
                                    return result166(registers);
                                }
                                return result137(registers);
                            }
                            return result138(registers);
                        }
                        return result139(registers);
                    }
                    if (registers.useArnRegion != null) { // node 838
                        if (!registers.useArnRegion) { // node 839
                            if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 840
                                return result167(registers);
                            }
                            return result131(registers);
                        }
                        return result167(registers);
                    }
                    return result167(registers);
                }
                return result167(registers);
            }
            return result168(registers);
        }
        return result169(registers);
    }
    private static RuleResult result201(Registers registers) {
        // this is a subgraph that replaces node 834 and its subnodes: #<Set: {835, 836, 837, 841, 842, 843, 844, 845, 846, 847, 848, 849, 838, 839, 840}>
        if ("s3-outposts".equals(registers.bucketArn.service())) { // node 834
            registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
            if (registers.outpostId_ssa_1 != null) { // node 835
                if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false)) { // node 836
                    registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
                    if (registers.bucketPartition != null) { // node 837
                        if (registers.useArnRegion != null) { // node 841
                            if (!registers.useArnRegion) { // node 842
                                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 843
                                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 844
                                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 845
                                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 846
                                                registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                                if (registers.outpostType != null) { // node 847
                                                    registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                                    if (registers.accessPointName_ssa_2 != null) { // node 848
                                                        if ("accesspoint".equals(registers.outpostType)) { // node 849
                                                            return result162(registers);
                                                        }
                                                        return result164(registers);
                                                    }
                                                    return result165(registers);
                                                }
                                                return result166(registers);
                                            }
                                            return result137(registers);
                                        }
                                        return result138(registers);
                                    }
                                    return result139(registers);
                                }
                                return result131(registers);
                            }
                            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 844
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 845
                                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 846
                                        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                        if (registers.outpostType != null) { // node 847
                                            registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                            if (registers.accessPointName_ssa_2 != null) { // node 848
                                                if ("accesspoint".equals(registers.outpostType)) { // node 849
                                                    return result162(registers);
                                                }
                                                return result164(registers);
                                            }
                                            return result165(registers);
                                        }
                                        return result166(registers);
                                    }
                                    return result137(registers);
                                }
                                return result138(registers);
                            }
                            return result139(registers);
                        }
                        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 844
                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 845
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 846
                                    registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                                    if (registers.outpostType != null) { // node 847
                                        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                                        if (registers.accessPointName_ssa_2 != null) { // node 848
                                            if ("accesspoint".equals(registers.outpostType)) { // node 849
                                                return result162(registers);
                                            }
                                            return result164(registers);
                                        }
                                        return result165(registers);
                                    }
                                    return result166(registers);
                                }
                                return result137(registers);
                            }
                            return result138(registers);
                        }
                        return result139(registers);
                    }
                    if (registers.useArnRegion != null) { // node 838
                        if (!registers.useArnRegion) { // node 839
                            if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 840
                                return result167(registers);
                            }
                            return result131(registers);
                        }
                        return result167(registers);
                    }
                    return result167(registers);
                }
                return result167(registers);
            }
            return result168(registers);
        }
        return result169(registers);
    }
    private static RuleResult result202(Registers registers) {
        // this is a subgraph that replaces node 292 and its subnodes: #<Set: {294, 295, 296, 297, 304, 298, 299, 458, 459, 300, 301, 303, 302, 293}>
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        if (registers.partitionResult != null) { // node 292
            if (registers.accelerate) { // node 294
                return result0(registers);
            }
            if ("aws-cn".equals(registers.partitionResult.name())) { // node 295
                return result4(registers);
            }
            if (registers.useS3ExpressControlEndpoint != null) { // node 296
                if (Boolean.FALSE != registers.useS3ExpressControlEndpoint) { // node 297
                    if (registers.useDualStack) { // node 304
                        return result97(registers);
                    }
                    return result98(registers);
                }
                if (registers.useObjectLambdaEndpoint != null) { // node 298
                    if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 299
                        if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 458
                            if (registers.useDualStack) { // node 459
                                return result128(registers);
                            }
                            return result186(registers);
                        }
                        return result127(registers);
                    }
                    if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 300
                        if (registers.useDualStack) { // node 301
                            if ("aws-global".equals(registers.region)) { // node 303
                                return result188(registers);
                            }
                            return result189(registers);
                        }
                        if ("aws-global".equals(registers.region)) { // node 302
                            return result190(registers);
                        }
                        return result191(registers);
                    }
                    return result127(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 300
                    if (registers.useDualStack) { // node 301
                        if ("aws-global".equals(registers.region)) { // node 303
                            return result188(registers);
                        }
                        return result189(registers);
                    }
                    if ("aws-global".equals(registers.region)) { // node 302
                        return result190(registers);
                    }
                    return result191(registers);
                }
                return result127(registers);
            }
            if (registers.useObjectLambdaEndpoint != null) { // node 298
                if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 299
                    if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 458
                        if (registers.useDualStack) { // node 459
                            return result128(registers);
                        }
                        return result186(registers);
                    }
                    return result127(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 300
                    if (registers.useDualStack) { // node 301
                        if ("aws-global".equals(registers.region)) { // node 303
                            return result188(registers);
                        }
                        return result189(registers);
                    }
                    if ("aws-global".equals(registers.region)) { // node 302
                        return result190(registers);
                    }
                    return result191(registers);
                }
                return result127(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 300
                if (registers.useDualStack) { // node 301
                    if ("aws-global".equals(registers.region)) { // node 303
                        return result188(registers);
                    }
                    return result189(registers);
                }
                if ("aws-global".equals(registers.region)) { // node 302
                    return result190(registers);
                }
                return result191(registers);
            }
            return result127(registers);
        }
        if (registers.accelerate) { // node 293
            return result0(registers);
        }
        return result199(registers);
    }
    private static RuleResult result203(Registers registers) {
        // this is a subgraph that replaces node 928 and its subnodes: #<Set: {929, 930, 939, 940, 931, 932, 933, 934, 935, 936, 937, 938}>
        if (registers.useDualStack) { // node 928
            return result1(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 929
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 930
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 939
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 940
                        return result39(registers);
                    }
                    return result43(registers);
                }
                return result43(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 931
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 932
                    return result39(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 933
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 934
                        return result47(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                            return result51(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                                return result55(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                            return result55(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                        return result51(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                            return result55(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 933
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 934
                    return result47(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                        return result51(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                            return result55(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                    return result51(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                    return result55(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 931
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 932
                return result39(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 933
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 934
                    return result47(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                        return result51(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                            return result55(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                    return result51(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                    return result55(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 933
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 934
                return result47(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                    return result51(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                        return result55(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                    return result55(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 935
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 936
                return result51(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                    return result55(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 937
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 938
                return result55(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result204(Registers registers) {
        // this is a subgraph that replaces node 942 and its subnodes: #<Set: {943, 944, 953, 954, 945, 946, 947, 948, 949, 950, 951, 952}>
        if (registers.useDualStack) { // node 942
            return result1(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 943
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 944
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 953
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 954
                        return result18(registers);
                    }
                    return result22(registers);
                }
                return result22(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 945
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 946
                    return result18(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 947
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 948
                        return result26(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                            return result30(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                                return result34(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                            return result34(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                        return result30(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                            return result34(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 947
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 948
                    return result26(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                        return result30(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                            return result34(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                    return result30(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                    return result34(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 945
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 946
                return result18(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 947
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 948
                    return result26(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                        return result30(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                            return result34(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                    return result30(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                    return result34(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 947
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 948
                return result26(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                    return result30(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                        return result34(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                    return result34(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 949
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 950
                return result30(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                    return result34(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 951
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 952
                return result34(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result205(Registers registers) {
        // this is a subgraph that replaces node 11 and its subnodes: #<Set: {12, 21, 13, 14, 289, 290, 15, 16, 20, 17, 18, 19}>
        if (registers.useS3ExpressControlEndpoint != null) { // node 11
            if (Boolean.FALSE != registers.useS3ExpressControlEndpoint) { // node 12
                if (registers.useDualStack) { // node 21
                    return result99(registers);
                }
                return result100(registers);
            }
            if (registers.useObjectLambdaEndpoint != null) { // node 13
                if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 14
                    if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 289
                        if (registers.useDualStack) { // node 290
                            return result128(registers);
                        }
                        return result129(registers);
                    }
                    return result127(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                    if (registers.useDualStack) { // node 16
                        if ("aws-global".equals(registers.region)) { // node 20
                            return result192(registers);
                        }
                        return result193(registers);
                    }
                    if ("aws-global".equals(registers.region)) { // node 17
                        return result196(registers);
                    }
                    if (registers.useGlobalEndpoint) { // node 18
                        if ("us-east-1".equals(registers.region)) { // node 19
                            return result197(registers);
                        }
                        return result198(registers);
                    }
                    return result198(registers);
                }
                return result127(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                if (registers.useDualStack) { // node 16
                    if ("aws-global".equals(registers.region)) { // node 20
                        return result192(registers);
                    }
                    return result193(registers);
                }
                if ("aws-global".equals(registers.region)) { // node 17
                    return result196(registers);
                }
                if (registers.useGlobalEndpoint) { // node 18
                    if ("us-east-1".equals(registers.region)) { // node 19
                        return result197(registers);
                    }
                    return result198(registers);
                }
                return result198(registers);
            }
            return result127(registers);
        }
        if (registers.useObjectLambdaEndpoint != null) { // node 13
            if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 14
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 289
                    if (registers.useDualStack) { // node 290
                        return result128(registers);
                    }
                    return result129(registers);
                }
                return result127(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                if (registers.useDualStack) { // node 16
                    if ("aws-global".equals(registers.region)) { // node 20
                        return result192(registers);
                    }
                    return result193(registers);
                }
                if ("aws-global".equals(registers.region)) { // node 17
                    return result196(registers);
                }
                if (registers.useGlobalEndpoint) { // node 18
                    if ("us-east-1".equals(registers.region)) { // node 19
                        return result197(registers);
                    }
                    return result198(registers);
                }
                return result198(registers);
            }
            return result127(registers);
        }
        if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
            if (registers.useDualStack) { // node 16
                if ("aws-global".equals(registers.region)) { // node 20
                    return result192(registers);
                }
                return result193(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 17
                return result196(registers);
            }
            if (registers.useGlobalEndpoint) { // node 18
                if ("us-east-1".equals(registers.region)) { // node 19
                    return result197(registers);
                }
                return result198(registers);
            }
            return result198(registers);
        }
        return result127(registers);
    }
    private static RuleResult result206(Registers registers) {
        // this is a subgraph that replaces node 7 and its subnodes: #<Set: {8, 21, 9, 10, 113, 114, 15, 16, 20, 17, 18, 19}>
        if (registers.useS3ExpressControlEndpoint != null) { // node 7
            if (Boolean.FALSE != registers.useS3ExpressControlEndpoint) { // node 8
                if (registers.useDualStack) { // node 21
                    return result99(registers);
                }
                return result100(registers);
            }
            if (registers.useObjectLambdaEndpoint != null) { // node 9
                if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 10
                    if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 113
                        if (registers.useDualStack) { // node 114
                            return result128(registers);
                        }
                        return result187(registers);
                    }
                    return result127(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                    if (registers.useDualStack) { // node 16
                        if ("aws-global".equals(registers.region)) { // node 20
                            return result192(registers);
                        }
                        return result193(registers);
                    }
                    if ("aws-global".equals(registers.region)) { // node 17
                        return result196(registers);
                    }
                    if (registers.useGlobalEndpoint) { // node 18
                        if ("us-east-1".equals(registers.region)) { // node 19
                            return result197(registers);
                        }
                        return result198(registers);
                    }
                    return result198(registers);
                }
                return result127(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                if (registers.useDualStack) { // node 16
                    if ("aws-global".equals(registers.region)) { // node 20
                        return result192(registers);
                    }
                    return result193(registers);
                }
                if ("aws-global".equals(registers.region)) { // node 17
                    return result196(registers);
                }
                if (registers.useGlobalEndpoint) { // node 18
                    if ("us-east-1".equals(registers.region)) { // node 19
                        return result197(registers);
                    }
                    return result198(registers);
                }
                return result198(registers);
            }
            return result127(registers);
        }
        if (registers.useObjectLambdaEndpoint != null) { // node 9
            if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 10
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 113
                    if (registers.useDualStack) { // node 114
                        return result128(registers);
                    }
                    return result187(registers);
                }
                return result127(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
                if (registers.useDualStack) { // node 16
                    if ("aws-global".equals(registers.region)) { // node 20
                        return result192(registers);
                    }
                    return result193(registers);
                }
                if ("aws-global".equals(registers.region)) { // node 17
                    return result196(registers);
                }
                if (registers.useGlobalEndpoint) { // node 18
                    if ("us-east-1".equals(registers.region)) { // node 19
                        return result197(registers);
                    }
                    return result198(registers);
                }
                return result198(registers);
            }
            return result127(registers);
        }
        if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 15
            if (registers.useDualStack) { // node 16
                if ("aws-global".equals(registers.region)) { // node 20
                    return result192(registers);
                }
                return result193(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 17
                return result196(registers);
            }
            if (registers.useGlobalEndpoint) { // node 18
                if ("us-east-1".equals(registers.region)) { // node 19
                    return result197(registers);
                }
                return result198(registers);
            }
            return result198(registers);
        }
        return result127(registers);
    }
    private static RuleResult result207(Registers registers) {
        // this is a subgraph that replaces node 564 and its subnodes: #<Set: {565, 574, 575, 566, 567, 568, 569, 570, 571, 572, 573}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 564
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 565
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 574
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 575
                        return result15(registers);
                    }
                    return result19(registers);
                }
                return result19(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 566
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 567
                    return result15(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 568
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 569
                        return result23(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                            return result27(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                                return result31(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                            return result31(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                        return result27(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                            return result31(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 568
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 569
                    return result23(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                        return result27(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                            return result31(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                    return result27(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                    return result31(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 566
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 567
                return result15(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 568
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 569
                    return result23(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                        return result27(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                            return result31(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                    return result27(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                    return result31(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 568
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 569
                return result23(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                    return result27(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                        return result31(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                    return result31(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 570
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 571
                return result27(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                    return result31(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 572
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 573
                return result31(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result208(Registers registers) {
        // this is a subgraph that replaces node 552 and its subnodes: #<Set: {553, 562, 563, 554, 555, 556, 557, 558, 559, 560, 561}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 552
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 553
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 562
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 563
                        return result16(registers);
                    }
                    return result20(registers);
                }
                return result20(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 554
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 555
                    return result16(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 556
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 557
                        return result24(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                            return result28(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                                return result32(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                            return result32(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                        return result28(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                            return result32(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 556
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 557
                    return result24(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                        return result28(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                            return result32(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                    return result28(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                    return result32(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 554
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 555
                return result16(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 556
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 557
                    return result24(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                        return result28(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                            return result32(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                    return result28(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                    return result32(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 556
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 557
                return result24(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                    return result28(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                        return result32(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                    return result32(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 558
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 559
                return result28(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                    return result32(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 560
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 561
                return result32(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result209(Registers registers) {
        // this is a subgraph that replaces node 536 and its subnodes: #<Set: {537, 546, 547, 538, 539, 540, 541, 542, 543, 544, 545}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 536
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 537
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 546
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 547
                        return result36(registers);
                    }
                    return result40(registers);
                }
                return result40(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 538
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 539
                    return result36(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 540
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 541
                        return result44(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                            return result48(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                                return result52(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                            return result52(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                        return result48(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                            return result52(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 540
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 541
                    return result44(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                        return result48(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                            return result52(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                    return result48(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                    return result52(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 538
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 539
                return result36(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 540
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 541
                    return result44(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                        return result48(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                            return result52(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                    return result48(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                    return result52(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 540
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 541
                return result44(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                    return result48(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                        return result52(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                    return result52(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 542
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 543
                return result48(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                    return result52(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 544
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 545
                return result52(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result210(Registers registers) {
        // this is a subgraph that replaces node 524 and its subnodes: #<Set: {525, 534, 535, 526, 527, 528, 529, 530, 531, 532, 533}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 524
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 525
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 534
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 535
                        return result37(registers);
                    }
                    return result41(registers);
                }
                return result41(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 526
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 527
                    return result37(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 528
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 529
                        return result45(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                            return result49(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                                return result53(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                            return result53(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                        return result49(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                            return result53(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 528
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 529
                    return result45(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                        return result49(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                            return result53(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                    return result49(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                    return result53(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 526
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 527
                return result37(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 528
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 529
                    return result45(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                        return result49(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                            return result53(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                    return result49(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                    return result53(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 528
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 529
                return result45(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                    return result49(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                        return result53(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                    return result53(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 530
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 531
                return result49(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                    return result53(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 532
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 533
                return result53(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result211(Registers registers) {
        // this is a subgraph that replaces node 173 and its subnodes: #<Set: {174, 183, 184, 175, 176, 177, 178, 179, 180, 181, 182}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 173
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 174
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 183
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 184
                        return result17(registers);
                    }
                    return result21(registers);
                }
                return result21(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 175
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 176
                    return result17(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 177
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 178
                        return result25(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                            return result29(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                                return result33(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                            return result33(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                        return result29(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                            return result33(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 177
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 178
                    return result25(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                        return result29(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                            return result33(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                    return result29(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                    return result33(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 175
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 176
                return result17(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 177
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 178
                    return result25(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                        return result29(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                            return result33(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                    return result29(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                    return result33(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 177
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 178
                return result25(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                    return result29(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                        return result33(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                    return result33(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 179
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 180
                return result29(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                    return result33(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 181
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 182
                return result33(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result212(Registers registers) {
        // this is a subgraph that replaces node 157 and its subnodes: #<Set: {158, 167, 168, 159, 160, 161, 162, 163, 164, 165, 166}>
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_2 != null) { // node 157
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 158
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
                if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 167
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 168
                        return result38(registers);
                    }
                    return result42(registers);
                }
                return result42(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 159
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 160
                    return result38(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
                if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 161
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 162
                        return result46(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                            return result50(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                                return result54(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                            return result54(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                        return result50(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                            return result54(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 161
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 162
                    return result46(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                        return result50(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                            return result54(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                    return result50(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                    return result54(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        if (registers.s3expressAvailabilityZoneId_ssa_1 != null) { // node 159
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), ""))) { // node 160
                return result38(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 161
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 162
                    return result46(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                        return result50(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                            return result54(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                    return result50(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                    return result54(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        if (registers.s3expressAvailabilityZoneId_ssa_3 != null) { // node 161
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), ""))) { // node 162
                return result46(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                    return result50(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
                if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                        return result54(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                    return result54(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_4 != null) { // node 163
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 164
                return result50(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                    return result54(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        if (registers.s3expressAvailabilityZoneId_ssa_5 != null) { // node 165
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), ""))) { // node 166
                return result54(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result213(Registers registers) {
        // this is a subgraph that replaces node 886 and its subnodes: #<Set: {888, 890, 894, 895, 896, 891, 892, 893, 889, 887}>
        registers.url = RulesFunctions.parseURL(registers.endpoint);
        if (registers.url != null) { // node 886
            if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false)) { // node 888
                if ("beta".equals(registers.regionPrefix)) { // node 890
                    if (registers.useDualStack) { // node 894
                        return result1(registers);
                    }
                    if ("e".equals(registers.hardwareType)) { // node 895
                        return result102(registers);
                    }
                    if ("o".equals(registers.hardwareType)) { // node 896
                        return result104(registers);
                    }
                    return result106(registers);
                }
                if (registers.useDualStack) { // node 891
                    return result1(registers);
                }
                if ("e".equals(registers.hardwareType)) { // node 892
                    return result103(registers);
                }
                if ("o".equals(registers.hardwareType)) { // node 893
                    return result105(registers);
                }
                return result106(registers);
            }
            if (registers.useDualStack) { // node 889
                return result1(registers);
            }
            return result107(registers);
        }
        if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false)) { // node 887
            if (registers.useDualStack) { // node 891
                return result1(registers);
            }
            if ("e".equals(registers.hardwareType)) { // node 892
                return result103(registers);
            }
            if ("o".equals(registers.hardwareType)) { // node 893
                return result105(registers);
            }
            return result106(registers);
        }
        if (registers.useDualStack) { // node 889
            return result1(registers);
        }
        return result107(registers);
    }
    private static RuleResult result214(Registers registers) {
        // this is a subgraph that replaces node 913 and its subnodes: #<Set: {914, 915, 916, 917, 918, 919, 920, 921, 922, 923}>
        if (registers.useDualStack) { // node 913
            return result1(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 914
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 915
                return result59(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 916
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 917
                    return result63(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 918
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 919
                        return result67(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                            return result71(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                                return result75(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                            return result75(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                        return result71(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                            return result75(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 918
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 919
                    return result67(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                        return result71(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                            return result75(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                    return result71(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                    return result75(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 916
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 917
                return result63(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 918
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 919
                    return result67(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                        return result71(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                            return result75(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                    return result71(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                    return result75(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 918
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 919
                return result67(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                    return result71(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                        return result75(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                    return result75(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 920
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 921
                return result71(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                    return result75(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 922
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 923
                return result75(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result215(Registers registers) {
        // this is a subgraph that replaces node 109 and its subnodes: #<Set: {110, 115, 119, 116, 117, 118, 111, 112, 113, 114}>
        if (RulesFunctions.awsParseArn(registers.bucket) != null) { // node 109
            return result172(registers);
        }
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        if (registers.uri_encoded_bucket != null) { // node 110
            if (registers.useDualStack) { // node 115
                if ("aws-global".equals(registers.region)) { // node 119
                    return result177(registers);
                }
                return result178(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 116
                return result181(registers);
            }
            if (registers.useGlobalEndpoint) { // node 117
                if ("us-east-1".equals(registers.region)) { // node 118
                    return result182(registers);
                }
                return result183(registers);
            }
            return result183(registers);
        }
        if (registers.useObjectLambdaEndpoint != null) { // node 111
            if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 112
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 113
                    if (registers.useDualStack) { // node 114
                        return result128(registers);
                    }
                    return result187(registers);
                }
                return result127(registers);
            }
            return result199(registers);
        }
        return result199(registers);
    }
    private static RuleResult result216(Registers registers) {
        // this is a subgraph that replaces node 901 and its subnodes: #<Set: {902, 903, 904, 905, 906, 907, 908, 909, 910, 911}>
        if (registers.useDualStack) { // node 901
            return result1(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 902
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 903
                return result79(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 904
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 905
                    return result83(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 906
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 907
                        return result87(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                            return result91(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                                return result95(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                            return result95(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                        return result91(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                            return result95(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 906
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 907
                    return result87(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                        return result91(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                            return result95(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                    return result91(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                    return result95(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 904
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 905
                return result83(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 906
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 907
                    return result87(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                        return result91(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                            return result95(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                    return result91(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                    return result95(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 906
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 907
                return result87(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                    return result91(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                        return result95(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                    return result95(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 908
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 909
                return result91(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                    return result95(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 910
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 911
                return result95(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result217(Registers registers) {
        // this is a subgraph that replaces node 471 and its subnodes: #<Set: {472, 473, 474, 475, 476, 477, 478, 479, 480}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 471
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 472
                return result77(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 473
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 474
                    return result81(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 475
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 476
                        return result85(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                            return result89(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                                return result93(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                            return result93(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                        return result89(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                            return result93(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 475
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 476
                    return result85(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                        return result89(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                            return result93(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                    return result89(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                    return result93(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 473
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 474
                return result81(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 475
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 476
                    return result85(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                        return result89(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                            return result93(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                    return result89(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                    return result93(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 475
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 476
                return result85(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                    return result89(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                        return result93(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                    return result93(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 477
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 478
                return result89(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                    return result93(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 479
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 480
                return result93(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result218(Registers registers) {
        // this is a subgraph that replaces node 481 and its subnodes: #<Set: {482, 483, 484, 485, 486, 487, 488, 489, 490}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 481
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 482
                return result76(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 483
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 484
                    return result80(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 485
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 486
                        return result84(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                            return result88(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                                return result92(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                            return result92(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                        return result88(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                            return result92(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 485
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 486
                    return result84(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                        return result88(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                            return result92(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                    return result88(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                    return result92(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 483
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 484
                return result80(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 485
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 486
                    return result84(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                        return result88(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                            return result92(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                    return result88(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                    return result92(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 485
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 486
                return result84(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                    return result88(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                        return result92(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                    return result92(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 487
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 488
                return result88(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                    return result92(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 489
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 490
                return result92(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result219(Registers registers) {
        // this is a subgraph that replaces node 493 and its subnodes: #<Set: {494, 495, 496, 497, 498, 499, 500, 501, 502}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 493
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 494
                return result57(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 495
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 496
                    return result61(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 497
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 498
                        return result65(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                            return result69(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                                return result73(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                            return result73(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                        return result69(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                            return result73(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 497
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 498
                    return result65(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                        return result69(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                            return result73(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                    return result69(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                    return result73(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 495
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 496
                return result61(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 497
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 498
                    return result65(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                        return result69(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                            return result73(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                    return result69(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                    return result73(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 497
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 498
                return result65(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                    return result69(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                        return result73(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                    return result73(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 499
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 500
                return result69(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                    return result73(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 501
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 502
                return result73(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result220(Registers registers) {
        // this is a subgraph that replaces node 503 and its subnodes: #<Set: {504, 505, 506, 507, 508, 509, 510, 511, 512}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 503
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 504
                return result56(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 505
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 506
                    return result60(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 507
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 508
                        return result64(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                            return result68(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                                return result72(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                            return result72(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                        return result68(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                            return result72(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 507
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 508
                    return result64(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                        return result68(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                            return result72(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                    return result68(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                    return result72(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 505
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 506
                return result60(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 507
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 508
                    return result64(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                        return result68(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                            return result72(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                    return result68(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                    return result72(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 507
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 508
                return result64(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                    return result68(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                        return result72(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                    return result72(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 509
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 510
                return result68(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                    return result72(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 511
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 512
                return result72(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result221(Registers registers) {
        // this is a subgraph that replaces node 124 and its subnodes: #<Set: {125, 126, 127, 128, 129, 130, 131, 132, 133}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 124
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 125
                return result78(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 126
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 127
                    return result82(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 128
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 129
                        return result86(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                            return result90(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                                return result94(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                            return result94(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                        return result90(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                            return result94(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 128
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 129
                    return result86(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                        return result90(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                            return result94(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                    return result90(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                    return result94(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 126
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 127
                return result82(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 128
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 129
                    return result86(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                        return result90(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                            return result94(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                    return result90(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                    return result94(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 128
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 129
                return result86(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                    return result90(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                        return result94(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                    return result94(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 130
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 131
                return result90(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                    return result94(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 132
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 133
                return result94(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result222(Registers registers) {
        // this is a subgraph that replaces node 136 and its subnodes: #<Set: {137, 138, 139, 140, 141, 142, 143, 144, 145}>
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        if (registers.s3expressAvailabilityZoneId_ssa_6 != null) { // node 136
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), ""))) { // node 137
                return result58(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 138
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 139
                    return result62(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
                if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 140
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 141
                        return result66(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                            return result70(registers);
                        }
                        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                                return result74(registers);
                            }
                            return result35(registers);
                        }
                        return result35(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                            return result74(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                        return result70(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                            return result74(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 140
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 141
                    return result66(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                        return result70(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                            return result74(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                    return result70(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                    return result74(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        if (registers.s3expressAvailabilityZoneId_ssa_7 != null) { // node 138
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), ""))) { // node 139
                return result62(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 140
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 141
                    return result66(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
                if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                        return result70(registers);
                    }
                    registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                    if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                        if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                            return result74(registers);
                        }
                        return result35(registers);
                    }
                    return result35(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                    return result70(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                    return result74(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        if (registers.s3expressAvailabilityZoneId_ssa_8 != null) { // node 140
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), ""))) { // node 141
                return result66(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                    return result70(registers);
                }
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
                if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                    if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                        return result74(registers);
                    }
                    return result35(registers);
                }
                return result35(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                    return result74(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        if (registers.s3expressAvailabilityZoneId_ssa_9 != null) { // node 142
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), ""))) { // node 143
                return result70(registers);
            }
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
                if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                    return result74(registers);
                }
                return result35(registers);
            }
            return result35(registers);
        }
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        if (registers.s3expressAvailabilityZoneId_ssa_10 != null) { // node 144
            if ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), ""))) { // node 145
                return result74(registers);
            }
            return result35(registers);
        }
        return result35(registers);
    }
    private static RuleResult result223(Registers registers) {
        // this is a subgraph that replaces node 454 and its subnodes: #<Set: {455, 460, 462, 461, 456, 457, 458, 459}>
        if (RulesFunctions.awsParseArn(registers.bucket) != null) { // node 454
            return result172(registers);
        }
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        if (registers.uri_encoded_bucket != null) { // node 455
            if (registers.useDualStack) { // node 460
                if ("aws-global".equals(registers.region)) { // node 462
                    return result173(registers);
                }
                return result174(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 461
                return result175(registers);
            }
            return result176(registers);
        }
        if (registers.useObjectLambdaEndpoint != null) { // node 456
            if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 457
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 458
                    if (registers.useDualStack) { // node 459
                        return result128(registers);
                    }
                    return result186(registers);
                }
                return result127(registers);
            }
            return result199(registers);
        }
        return result199(registers);
    }
    private static RuleResult result224(Registers registers) {
        // this is a subgraph that replaces node 801 and its subnodes: #<Set: {802, 803, 804, 805, 806, 807, 808}>
        if (registers.useArnRegion != null) { // node 801
            if (!registers.useArnRegion) { // node 802
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 803
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 804
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 805
                            if ("s3".equals(registers.bucketArn.service())) { // node 806
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 807
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 808
                                        return result148(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 804
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 805
                    if ("s3".equals(registers.bucketArn.service())) { // node 806
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 807
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 808
                                return result148(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 804
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 805
                if ("s3".equals(registers.bucketArn.service())) { // node 806
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 807
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 808
                            return result148(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result225(Registers registers) {
        // this is a subgraph that replaces node 276 and its subnodes: #<Set: {277, 278, 279, 280, 281, 282, 283}>
        if (registers.useArnRegion != null) { // node 276
            if (!registers.useArnRegion) { // node 277
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 278
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 279
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 280
                            if ("s3".equals(registers.bucketArn.service())) { // node 281
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 282
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 283
                                        return result144(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 279
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 280
                    if ("s3".equals(registers.bucketArn.service())) { // node 281
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 282
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 283
                                return result144(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 279
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 280
                if ("s3".equals(registers.bucketArn.service())) { // node 281
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 282
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 283
                            return result144(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result226(Registers registers) {
        // this is a subgraph that replaces node 391 and its subnodes: #<Set: {392, 393, 394, 395, 396, 397, 398}>
        if (registers.useArnRegion != null) { // node 391
            if (!registers.useArnRegion) { // node 392
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 393
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 394
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 395
                            if ("".equals(registers.bucketArn.accountId())) { // node 396
                                return result132(registers);
                            }
                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 397
                                if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 398
                                    return result134(registers);
                                }
                                return result136(registers);
                            }
                            return result137(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 394
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 395
                    if ("".equals(registers.bucketArn.accountId())) { // node 396
                        return result132(registers);
                    }
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 397
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 398
                            return result134(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 394
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 395
                if ("".equals(registers.bucketArn.accountId())) { // node 396
                    return result132(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 397
                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 398
                        return result134(registers);
                    }
                    return result136(registers);
                }
                return result137(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result227(Registers registers) {
        // this is a subgraph that replaces node 380 and its subnodes: #<Set: {381, 382, 383, 384, 385, 386, 387}>
        if (registers.useArnRegion != null) { // node 380
            if (!registers.useArnRegion) { // node 381
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 382
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 383
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 384
                            if ("s3".equals(registers.bucketArn.service())) { // node 385
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 386
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 387
                                        return result146(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 383
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 384
                    if ("s3".equals(registers.bucketArn.service())) { // node 385
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 386
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 387
                                return result146(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 383
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 384
                if ("s3".equals(registers.bucketArn.service())) { // node 385
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 386
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 387
                            return result146(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result228(Registers registers) {
        // this is a subgraph that replaces node 424 and its subnodes: #<Set: {425, 426, 427, 428, 429, 430, 431}>
        if (registers.useArnRegion != null) { // node 424
            if (!registers.useArnRegion) { // node 425
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 426
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 427
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 428
                            if ("s3".equals(registers.bucketArn.service())) { // node 429
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 430
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 431
                                        return result145(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 427
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 428
                    if ("s3".equals(registers.bucketArn.service())) { // node 429
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 430
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 431
                                return result145(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 427
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 428
                if ("s3".equals(registers.bucketArn.service())) { // node 429
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 430
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 431
                            return result145(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result229(Registers registers) {
        // this is a subgraph that replaces node 95 and its subnodes: #<Set: {96, 97, 98, 99, 100, 101, 102}>
        if (registers.useArnRegion != null) { // node 95
            if (!registers.useArnRegion) { // node 96
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 97
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 98
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 99
                            if ("s3".equals(registers.bucketArn.service())) { // node 100
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 101
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 102
                                        return result147(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 98
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 99
                    if ("s3".equals(registers.bucketArn.service())) { // node 100
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 101
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 102
                                return result147(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 98
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 99
                if ("s3".equals(registers.bucketArn.service())) { // node 100
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 101
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 102
                            return result147(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result230(Registers registers) {
        // this is a subgraph that replaces node 743 and its subnodes: #<Set: {744, 745, 746, 747, 748, 749, 750}>
        if (registers.useArnRegion != null) { // node 743
            if (!registers.useArnRegion) { // node 744
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 745
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 746
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 747
                            if ("".equals(registers.bucketArn.accountId())) { // node 748
                                return result132(registers);
                            }
                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 749
                                if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 750
                                    return result135(registers);
                                }
                                return result136(registers);
                            }
                            return result137(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 746
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 747
                    if ("".equals(registers.bucketArn.accountId())) { // node 748
                        return result132(registers);
                    }
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 749
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 750
                            return result135(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 746
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 747
                if ("".equals(registers.bucketArn.accountId())) { // node 748
                    return result132(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 749
                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 750
                        return result135(registers);
                    }
                    return result136(registers);
                }
                return result137(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result231(Registers registers) {
        // this is a subgraph that replaces node 732 and its subnodes: #<Set: {733, 734, 735, 736, 737, 738, 739}>
        if (registers.useArnRegion != null) { // node 732
            if (!registers.useArnRegion) { // node 733
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 734
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 735
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 736
                            if ("s3".equals(registers.bucketArn.service())) { // node 737
                                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 738
                                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 739
                                        return result149(registers);
                                    }
                                    return result136(registers);
                                }
                                return result137(registers);
                            }
                            return result150(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 735
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 736
                    if ("s3".equals(registers.bucketArn.service())) { // node 737
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 738
                            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 739
                                return result149(registers);
                            }
                            return result136(registers);
                        }
                        return result137(registers);
                    }
                    return result150(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 735
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 736
                if ("s3".equals(registers.bucketArn.service())) { // node 737
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 738
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 739
                            return result149(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result150(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result232(Registers registers) {
        // this is a subgraph that replaces node 815 and its subnodes: #<Set: {816, 817, 818, 819, 820, 821, 822}>
        if (registers.useArnRegion != null) { // node 815
            if (!registers.useArnRegion) { // node 816
                if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 817
                    if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 818
                        if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 819
                            if ("".equals(registers.bucketArn.accountId())) { // node 820
                                return result132(registers);
                            }
                            if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 821
                                if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 822
                                    return result133(registers);
                                }
                                return result136(registers);
                            }
                            return result137(registers);
                        }
                        return result138(registers);
                    }
                    return result139(registers);
                }
                return result131(registers);
            }
            if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 818
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 819
                    if ("".equals(registers.bucketArn.accountId())) { // node 820
                        return result132(registers);
                    }
                    if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 821
                        if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 822
                            return result133(registers);
                        }
                        return result136(registers);
                    }
                    return result137(registers);
                }
                return result138(registers);
            }
            return result139(registers);
        }
        if (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name())) { // node 818
            if (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true)) { // node 819
                if ("".equals(registers.bucketArn.accountId())) { // node 820
                    return result132(registers);
                }
                if (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false)) { // node 821
                    if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false)) { // node 822
                        return result133(registers);
                    }
                    return result136(registers);
                }
                return result137(registers);
            }
            return result138(registers);
        }
        return result139(registers);
    }
    private static RuleResult result233(Registers registers) {
        // this is a subgraph that replaces node 70 and its subnodes: #<Set: {72, 119, 71, 116, 117, 118}>
        if (registers.useDualStack) { // node 70
            if ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), ""))) { // node 72
                return result171(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 119
                return result177(registers);
            }
            return result178(registers);
        }
        if ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), ""))) { // node 71
            return result171(registers);
        }
        if ("aws-global".equals(registers.region)) { // node 116
            return result181(registers);
        }
        if (registers.useGlobalEndpoint) { // node 117
            if ("us-east-1".equals(registers.region)) { // node 118
                return result182(registers);
            }
            return result183(registers);
        }
        return result183(registers);
    }
    private static RuleResult result234(Registers registers) {
        // this is a subgraph that replaces node 636 and its subnodes: #<Set: {637, 638, 639, 838, 839, 840}>
        if ("s3-object-lambda".equals(registers.bucketArn.service())) { // node 636
            return result143(registers);
        }
        if ("s3-outposts".equals(registers.bucketArn.service())) { // node 637
            registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
            if (registers.outpostId_ssa_1 != null) { // node 638
                if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false)) { // node 639
                    if (registers.useArnRegion != null) { // node 838
                        if (!registers.useArnRegion) { // node 839
                            if (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region())) { // node 840
                                return result167(registers);
                            }
                            return result131(registers);
                        }
                        return result167(registers);
                    }
                    return result167(registers);
                }
                return result167(registers);
            }
            return result168(registers);
        }
        return result169(registers);
    }
    private static RuleResult result235(Registers registers) {
        // this is a subgraph that replaces node 285 and its subnodes: #<Set: {286, 287, 288, 289, 290}>
        if (RulesFunctions.awsParseArn(registers.bucket) != null) { // node 285
            return result172(registers);
        }
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        if (registers.uri_encoded_bucket != null) { // node 286
            return result184(registers);
        }
        if (registers.useObjectLambdaEndpoint != null) { // node 287
            if (Boolean.FALSE != registers.useObjectLambdaEndpoint) { // node 288
                if (RulesFunctions.isValidHostLabel(registers.region, true)) { // node 289
                    if (registers.useDualStack) { // node 290
                        return result128(registers);
                    }
                    return result129(registers);
                }
                return result127(registers);
            }
            return result199(registers);
        }
        return result199(registers);
    }
    private static RuleResult result236(Registers registers) {
        // this is a subgraph that replaces node 463 and its subnodes: #<Set: {464, 465, 466, 892, 893}>
        if (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false)) { // node 463
            if ("beta".equals(registers.regionPrefix)) { // node 464
                if ("e".equals(registers.hardwareType)) { // node 465
                    return result101(registers);
                }
                if ("o".equals(registers.hardwareType)) { // node 466
                    return result101(registers);
                }
                return result106(registers);
            }
            if ("e".equals(registers.hardwareType)) { // node 892
                return result103(registers);
            }
            if ("o".equals(registers.hardwareType)) { // node 893
                return result105(registers);
            }
            return result106(registers);
        }
        return result107(registers);
    }
    private static RuleResult result237(Registers registers) {
        // this is a subgraph that replaces node 103 and its subnodes: #<Set: {104, 108, 105, 106, 107}>
        if (RulesFunctions.isValidHostLabel(registers.region, false)) { // node 103
            if (registers.useDualStack) { // node 104
                if ("aws-global".equals(registers.region)) { // node 108
                    return result116(registers);
                }
                return result117(registers);
            }
            if ("aws-global".equals(registers.region)) { // node 105
                return result124(registers);
            }
            if (registers.useGlobalEndpoint) { // node 106
                if ("us-east-1".equals(registers.region)) { // node 107
                    return result125(registers);
                }
                return result126(registers);
            }
            return result126(registers);
        }
        return result127(registers);
    }
    private static RuleResult result238(Registers registers) {
        // this is a subgraph that replaces node 859 and its subnodes: #<Set: {860, 861, 864, 862, 863}>
        registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        if (registers.accessPointName_ssa_1 != null) { // node 859
            if ("".equals(registers.accessPointName_ssa_1)) { // node 860
                return result142(registers);
            }
            if (registers.disableMultiRegionAccessPoints) { // node 861
                if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true)) { // node 864
                    return result154(registers);
                }
                return result157(registers);
            }
            if (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true)) { // node 862
                if (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name())) { // node 863
                    return result155(registers);
                }
                return result156(registers);
            }
            return result157(registers);
        }
        return result142(registers);
    }


    private static boolean cond0(Registers registers) {
        return (registers.region != null);
    }

    private static boolean cond1(Registers registers) {
        return (registers.endpoint != null);
    }

    private static boolean cond2(Registers registers) {
        return (registers.useFIPS);
    }

    private static boolean cond3(Registers registers) {
        return (registers.bucket != null);
    }

    private static boolean cond4(Registers registers) {
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        return registers.partitionResult != null;
    }

    private static boolean cond5(Registers registers) {
        return (registers.accelerate);
    }

    private static boolean cond6(Registers registers) {
        return ("aws-cn".equals(registers.partitionResult.name()));
    }

    private static boolean cond7(Registers registers) {
        return (RulesFunctions.parseURL(registers.endpoint) != null);
    }

    private static boolean cond8(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond9(Registers registers) {
        return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 6, true), "")));
    }

    private static boolean cond10(Registers registers) {
        return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 7, true), "")));
    }

    private static boolean cond11(Registers registers) {
        return (registers.useS3ExpressControlEndpoint != null);
    }

    private static boolean cond12(Registers registers) {
        return (Boolean.FALSE != registers.useS3ExpressControlEndpoint);
    }

    private static boolean cond13(Registers registers) {
        registers.accessPointSuffix = RulesFunctions.substring(registers.bucket, 0, 7, true);
        return registers.accessPointSuffix != null;
    }

    private static boolean cond14(Registers registers) {
        return ("--op-s3".equals(registers.accessPointSuffix));
    }

    private static boolean cond15(Registers registers) {
        registers.regionPrefix = RulesFunctions.substring(registers.bucket, 8, 12, true);
        return registers.regionPrefix != null;
    }

    private static boolean cond16(Registers registers) {
        registers.outpostId_ssa_2 = RulesFunctions.substring(registers.bucket, 32, 49, true);
        return registers.outpostId_ssa_2 != null;
    }

    private static boolean cond17(Registers registers) {
        registers.hardwareType = RulesFunctions.substring(registers.bucket, 49, 50, true);
        return registers.hardwareType != null;
    }

    private static boolean cond18(Registers registers) {
        registers.url = RulesFunctions.parseURL(registers.endpoint);
        return registers.url != null;
    }

    private static boolean cond19(Registers registers) {
        return (registers.disableS3ExpressSessionAuth != null);
    }

    private static boolean cond20(Registers registers) {
        return (Boolean.FALSE != registers.disableS3ExpressSessionAuth);
    }

    private static boolean cond21(Registers registers) {
        return (registers.forcePathStyle);
    }

    private static boolean cond22(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, false));
    }

    private static boolean cond23(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond24(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, true));
    }

    private static boolean cond25(Registers registers) {
        return ("http".equals(registers.url.scheme()));
    }

    private static boolean cond26(Registers registers) {
        registers.bucketArn = RulesFunctions.awsParseArn(registers.bucket);
        return registers.bucketArn != null;
    }

    private static boolean cond27(Registers registers) {
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        return registers.uri_encoded_bucket != null;
    }

    private static boolean cond28(Registers registers) {
        return (registers.useObjectLambdaEndpoint != null);
    }

    private static boolean cond29(Registers registers) {
        return (Boolean.FALSE != registers.useObjectLambdaEndpoint);
    }

    private static boolean cond30(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, true));
    }

    private static boolean cond31(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
    }

    private static boolean cond32(Registers registers) {
        return ("beta".equals(registers.regionPrefix));
    }

    private static boolean cond33(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, false));
    }

    private static boolean cond34(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean cond35(Registers registers) {
        return (registers.url.isIp());
    }

    private static boolean cond36(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_6 != null;
    }

    private static boolean cond37(Registers registers) {
        return ("e".equals(registers.hardwareType));
    }

    private static boolean cond38(Registers registers) {
        return ("o".equals(registers.hardwareType));
    }

    private static boolean cond39(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_2 != null;
    }

    private static boolean cond40(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), "")));
    }

    private static boolean cond41(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        return registers.s3expressAvailabilityZoneId_ssa_7 != null;
    }

    private static boolean cond42(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), "")));
    }

    private static boolean cond43(Registers registers) {
        return (registers.disableAccessPoints != null);
    }

    private static boolean cond44(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        return registers.s3expressAvailabilityZoneId_ssa_1 != null;
    }

    private static boolean cond45(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), "")));
    }

    private static boolean cond46(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_8 != null;
    }

    private static boolean cond47(Registers registers) {
        return (Boolean.FALSE != registers.disableAccessPoints);
    }

    private static boolean cond48(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        return registers.s3expressAvailabilityZoneId_ssa_3 != null;
    }

    private static boolean cond49(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), "")));
    }

    private static boolean cond50(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_4 != null;
    }

    private static boolean cond51(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
    }

    private static boolean cond52(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), "")));
    }

    private static boolean cond53(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
    }

    private static boolean cond54(Registers registers) {
        registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
        return registers.arnType != null;
    }

    private static boolean cond55(Registers registers) {
        return ("".equals(registers.arnType));
    }

    private static boolean cond56(Registers registers) {
        return ("accesspoint".equals(registers.arnType));
    }

    private static boolean cond57(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        return registers.s3expressAvailabilityZoneId_ssa_9 != null;
    }

    private static boolean cond58(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), "")));
    }

    private static boolean cond59(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        return registers.s3expressAvailabilityZoneId_ssa_5 != null;
    }

    private static boolean cond60(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), "")));
    }

    private static boolean cond61(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        return registers.s3expressAvailabilityZoneId_ssa_10 != null;
    }

    private static boolean cond62(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), "")));
    }

    private static boolean cond63(Registers registers) {
        return ("".equals(registers.bucketArn.region()));
    }

    private static boolean cond64(Registers registers) {
        return ("s3-object-lambda".equals(registers.bucketArn.service()));
    }

    private static boolean cond65(Registers registers) {
        return ("s3-outposts".equals(registers.bucketArn.service()));
    }

    private static boolean cond66(Registers registers) {
        registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.outpostId_ssa_1 != null;
    }

    private static boolean cond67(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
    }

    private static boolean cond68(Registers registers) {
        registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.accessPointName_ssa_1 != null;
    }

    private static boolean cond69(Registers registers) {
        return ("".equals(registers.accessPointName_ssa_1));
    }

    private static boolean cond70(Registers registers) {
        registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
        return registers.bucketPartition != null;
    }

    private static boolean cond71(Registers registers) {
        return (registers.useArnRegion != null);
    }

    private static boolean cond72(Registers registers) {
        return (!registers.useArnRegion);
    }

    private static boolean cond73(Registers registers) {
        return (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region()));
    }

    private static boolean cond74(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
    }

    private static boolean cond75(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
    }

    private static boolean cond76(Registers registers) {
        return ("".equals(registers.bucketArn.accountId()));
    }

    private static boolean cond77(Registers registers) {
        return ("s3".equals(registers.bucketArn.service()));
    }

    private static boolean cond78(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
    }

    private static boolean cond79(Registers registers) {
        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
        return registers.outpostType != null;
    }

    private static boolean cond80(Registers registers) {
        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
        return registers.accessPointName_ssa_2 != null;
    }

    private static boolean cond81(Registers registers) {
        return ("accesspoint".equals(registers.outpostType));
    }

    private static boolean cond82(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
    }

    private static boolean cond83(Registers registers) {
        return (registers.disableMultiRegionAccessPoints);
    }

    private static boolean cond84(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
    }

    private static boolean cond85(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
    }

    private static boolean cond86(Registers registers) {
        return (!registers.url.isIp());
    }

    private static boolean cond87(Registers registers) {
        return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), "")));
    }

    private static boolean cond88(Registers registers) {
        return ("aws-global".equals(registers.region));
    }

    private static boolean cond89(Registers registers) {
        return (registers.useGlobalEndpoint);
    }

    private static boolean cond90(Registers registers) {
        return ("us-east-1".equals(registers.region));
    }

    private static RuleResult result0(Registers registers) {
        return RuleResult.error("Accelerate cannot be used with FIPS");
    }

    private static RuleResult result1(Registers registers) {
        return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
    }

    private static RuleResult result2(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with FIPS");
    }

    private static RuleResult result3(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
    }

    private static RuleResult result4(Registers registers) {
        return RuleResult.error("Partition does not support FIPS");
    }

    private static RuleResult result5(Registers registers) {
        return RuleResult.error("S3Express does not support S3 Accelerate.");
    }

    private static RuleResult result6(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/" + registers.uri_encoded_bucket
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result7(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result8(Registers registers) {
        return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
    }

    private static RuleResult result9(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/" + registers.uri_encoded_bucket
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result10(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result11(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result12(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result13(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result14(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                       + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result15(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result16(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result17(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result18(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result19(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result20(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result21(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result22(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result23(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result24(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result25(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result26(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result27(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result28(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result29(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result30(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result31(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result32(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result33(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result34(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result35(Registers registers) {
        return RuleResult.error("Unrecognized S3Express bucket name format.");
    }

    private static RuleResult result36(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result37(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result38(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result39(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result40(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result41(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result42(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result43(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result44(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result45(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result46(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result47(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result48(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result49(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result50(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result51(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result52(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result53(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result54(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result55(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result56(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result57(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result58(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result59(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result60(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result61(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result62(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result63(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result64(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result65(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result66(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result67(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result68(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result69(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result70(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result71(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result72(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result73(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result74(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result75(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result76(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result77(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result78(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result79(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result80(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result81(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result82(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result83(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result84(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result85(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result86(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result87(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result88(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result89(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result90(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result91(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9 + "."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result92(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result93(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-fips-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result94(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + ".dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result95(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                       + "." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result96(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result97(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result98(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result99(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result100(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result101(Registers registers) {
        return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
    }

    private static RuleResult result102(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".ec2." + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result103(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".ec2.s3-outposts." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result104(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + "."
                                                       + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result105(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + ".s3-outposts."
                                                       + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result106(Registers registers) {
        return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + registers.hardwareType
                                + "\"");
    }

    private static RuleResult result107(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
    }

    private static RuleResult result108(Registers registers) {
        return RuleResult.error("Custom endpoint `" + registers.endpoint + "` was not a valid URI");
    }

    private static RuleResult result109(Registers registers) {
        return RuleResult.error("S3 Accelerate cannot be used in this region");
    }

    private static RuleResult result110(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result111(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result112(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result113(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result114(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result115(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result116(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result117(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result118(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result119(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result120(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result121(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result122(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result123(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result124(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result125(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result126(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result127(Registers registers) {
        return RuleResult.error("Invalid region: region was not a valid DNS name.");
    }

    private static RuleResult result128(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support Dual-stack");
    }

    private static RuleResult result129(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
    }

    private static RuleResult result130(Registers registers) {
        return RuleResult.error("Access points are not supported for this operation");
    }

    private static RuleResult result131(Registers registers) {
        return RuleResult.error("Invalid configuration: region from ARN `" + registers.bucketArn.region()
                                + "` does not match client region `" + registers.region + "` and UseArnRegion is `false`");
    }

    private static RuleResult result132(Registers registers) {
        return RuleResult.error("Invalid ARN: Missing account id");
    }

    private static RuleResult result133(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result134(Registers registers) {
        return RuleResult
            .endpoint(Endpoint
                          .builder()
                          .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                          + ".s3-object-lambda-fips." + registers.bucketArn.region() + "."
                                          + registers.bucketPartition.dnsSuffix()))
                          .putAttribute(
                              AwsEndpointAttribute.AUTH_SCHEMES,
                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                           .signingName("s3-object-lambda").signingRegion(registers.bucketArn.region()).build()))
                          .build());
    }

    private static RuleResult result135(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-object-lambda." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result136(Registers registers) {
        return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.accessPointName_ssa_1 + "`");
    }

    private static RuleResult result137(Registers registers) {
        return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.bucketArn.accountId() + "`");
    }

    private static RuleResult result138(Registers registers) {
        return RuleResult.error("Invalid region in ARN: `" + registers.bucketArn.region() + "` (invalid DNS name)");
    }

    private static RuleResult result139(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name() + "` but ARN (`"
                                + registers.bucket + "`) has `" + registers.bucketPartition.name() + "`");
    }

    private static RuleResult result140(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
    }

    private static RuleResult result141(Registers registers) {
        return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
    }

    private static RuleResult result142(Registers registers) {
        return RuleResult
            .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
    }

    private static RuleResult result143(Registers registers) {
        return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                + registers.arnType + "`");
    }

    private static RuleResult result144(Registers registers) {
        return RuleResult.error("Access Points do not support S3 Accelerate");
    }

    private static RuleResult result145(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint-fips.dualstack." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result146(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint-fips." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result147(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint.dualstack." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result148(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result149(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result150(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + registers.bucketArn.service());
    }

    private static RuleResult result151(Registers registers) {
        return RuleResult.error("S3 MRAP does not support dual-stack");
    }

    private static RuleResult result152(Registers registers) {
        return RuleResult.error("S3 MRAP does not support FIPS");
    }

    private static RuleResult result153(Registers registers) {
        return RuleResult.error("S3 MRAP does not support S3 Accelerate");
    }

    private static RuleResult result154(Registers registers) {
        return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
    }

    private static RuleResult result155(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                         .signingRegionSet(Arrays.asList("*")).build())).build());
    }

    private static RuleResult result156(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                + "` but bucket referred to partition `" + registers.bucketArn.partition() + "`");
    }

    private static RuleResult result157(Registers registers) {
        return RuleResult.error("Invalid Access Point Name");
    }

    private static RuleResult result158(Registers registers) {
        return RuleResult.error("S3 Outposts does not support Dual-stack");
    }

    private static RuleResult result159(Registers registers) {
        return RuleResult.error("S3 Outposts does not support FIPS");
    }

    private static RuleResult result160(Registers registers) {
        return RuleResult.error("S3 Outposts does not support S3 Accelerate");
    }

    private static RuleResult result161(Registers registers) {
        return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
    }

    private static RuleResult result162(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                       + registers.outpostId_ssa_1 + "." + registers.url.authority()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result163(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                       + registers.outpostId_ssa_1 + ".s3-outposts." + registers.bucketArn.region() + "."
                                                       + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(
                                               SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                               .signingRegionSet(Arrays.asList("*")).build(),
                                               SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                              .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult result164(Registers registers) {
        return RuleResult.error("Expected an outpost type `accesspoint`, found " + registers.outpostType);
    }

    private static RuleResult result165(Registers registers) {
        return RuleResult.error("Invalid ARN: expected an access point name");
    }

    private static RuleResult result166(Registers registers) {
        return RuleResult.error("Invalid ARN: Expected a 4-component resource");
    }

    private static RuleResult result167(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.outpostId_ssa_1 + "`");
    }

    private static RuleResult result168(Registers registers) {
        return RuleResult.error("Invalid ARN: The Outpost Id was not set");
    }

    private static RuleResult result169(Registers registers) {
        return RuleResult.error("Invalid ARN: Unrecognized format: " + registers.bucket + " (type: " + registers.arnType + ")");
    }

    private static RuleResult result170(Registers registers) {
        return RuleResult.error("Invalid ARN: No ARN type specified");
    }

    private static RuleResult result171(Registers registers) {
        return RuleResult.error("Invalid ARN: `" + registers.bucket + "` was not a valid ARN");
    }

    private static RuleResult result172(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
    }

    private static RuleResult result173(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result174(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                       + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result175(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result176(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result177(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result178(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result179(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result180(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result181(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result182(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result183(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result184(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
    }

    private static RuleResult result185(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result186(Registers registers) {
        return RuleResult
            .endpoint(Endpoint
                          .builder()
                          .url(URI.create("https://s3-object-lambda-fips." + registers.region + "."
                                          + registers.partitionResult.dnsSuffix()))
                          .putAttribute(
                              AwsEndpointAttribute.AUTH_SCHEMES,
                              Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                           .signingName("s3-object-lambda").signingRegion(registers.region).build())).build());
    }

    private static RuleResult result187(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-object-lambda." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result188(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result189(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result190(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result191(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result192(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result193(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result194(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result195(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result196(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult result197(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result198(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult result199(Registers registers) {
        return RuleResult.error("A region must be set when sending requests to S3.");
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Registers registers = new Registers();
        registers.region = params.region() == null ? null : params.region().id();
        registers.bucket = params.bucket();
        registers.useFIPS = params.useFips();
        registers.useDualStack = params.useDualStack();
        registers.endpoint = params.endpoint();
        registers.forcePathStyle = params.forcePathStyle();
        registers.accelerate = params.accelerate();
        registers.useGlobalEndpoint = params.useGlobalEndpoint();
        registers.useObjectLambdaEndpoint = params.useObjectLambdaEndpoint();
        registers.key = params.key();
        registers.prefix = params.prefix();
        registers.copySource = params.copySource();
        registers.disableAccessPoints = params.disableAccessPoints();
        registers.disableMultiRegionAccessPoints = params.disableMultiRegionAccessPoints();
        registers.useArnRegion = params.useArnRegion();
        registers.useS3ExpressControlEndpoint = params.useS3ExpressControlEndpoint();
        registers.disableS3ExpressSessionAuth = params.disableS3ExpressSessionAuth();
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 2;
        while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000) {
            int base = (Math.abs(nodeRef) - 1) * 3;
            int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0;
            int conditionResult = CONDITION_FNS[bdd[base]].test(registers) ? 1 : 0;
            nodeRef = bdd[base + 2 - (complemented ^ conditionResult)];
        }
        if (nodeRef == -1 || nodeRef == 1) {
            return CompletableFutureUtils.failedFuture(SdkClientException
                                                           .create("Rule engine did not reach an error or endpoint result"));
        } else {
            RuleResult result = RESULT_FNS[nodeRef - 100000001].apply(registers);
            if (result.isError()) {
                String errorMsg = result.error();
                if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                    errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                }
                return CompletableFutureUtils.failedFuture(SdkClientException.create(errorMsg));
            }
            return CompletableFuture.completedFuture(result.endpoint());
        }
    }

    private static class Registers {
        String bucket;

        String region;

        boolean useFIPS;

        boolean useDualStack;

        String endpoint;

        boolean forcePathStyle;

        boolean accelerate;

        boolean useGlobalEndpoint;

        Boolean useObjectLambdaEndpoint;

        String key;

        String prefix;

        String copySource;

        Boolean disableAccessPoints;

        boolean disableMultiRegionAccessPoints;

        Boolean useArnRegion;

        Boolean useS3ExpressControlEndpoint;

        Boolean disableS3ExpressSessionAuth;

        RulePartition partitionResult;

        String accessPointSuffix;

        String regionPrefix;

        String outpostId_ssa_2;

        String hardwareType;

        RuleUrl url;

        RuleArn bucketArn;

        String uri_encoded_bucket;

        String s3expressAvailabilityZoneId_ssa_6;

        String s3expressAvailabilityZoneId_ssa_2;

        String s3expressAvailabilityZoneId_ssa_7;

        String s3expressAvailabilityZoneId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_8;

        String s3expressAvailabilityZoneId_ssa_3;

        String s3expressAvailabilityZoneId_ssa_4;

        String arnType;

        String s3expressAvailabilityZoneId_ssa_9;

        String s3expressAvailabilityZoneId_ssa_5;

        String s3expressAvailabilityZoneId_ssa_10;

        String outpostId_ssa_1;

        String accessPointName_ssa_1;

        RulePartition bucketPartition;

        String outpostType;

        String accessPointName_ssa_2;
    }

    @FunctionalInterface
    interface ConditionFn {
        boolean test(Registers registers);
    }

    @FunctionalInterface
    interface ResultFn {
        RuleResult apply(Registers registers);
    }
}
