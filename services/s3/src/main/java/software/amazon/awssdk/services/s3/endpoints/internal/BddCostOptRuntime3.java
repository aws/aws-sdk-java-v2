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
// uses dowlings new cost optimized BDD
// lambdas + boolean/loop optimization
public final class BddCostOptRuntime3 implements S3EndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1, 0, 3, 100000200, 4, 828, 4, 5, 569, 5, 1, 163, 6, 2, 12, 7, 15, 8,
                                                  100000200, 3, 9, 10, 8, 842, 10, 19, 11, 836, 21, 127, 836, 15, 24, 13, 16, 19, 14, 47, 100000009, 15, 53, 100000009,
                                                  16, 59, 17, 21, 10, 100000172, 18, 36, 23, 100000172, 47, 100000009, 20, 53, 100000009, 21, 10, 869, 22, 36, 23,
                                                  100000200, 26, 862, 200, 3, 25, 26, 8, 36, 26, 7, 27, 28, 9, 32, 28, 16, 30, 29, 47, 31, 40, 47, 31, 42, 12, 57,
                                                  100000009, 16, 34, 33, 47, 35, 72, 47, 35, 78, 12, 149, 100000009, 7, 37, 38, 9, 70, 38, 16, 41, 39, 47, 55, 40, 53,
                                                  43, 73, 47, 55, 42, 53, 43, 79, 12, 44, 100000009, 26, 45, 241, 27, 46, 47, 82, 100000079, 47, 83, 48, 49, 33,
                                                  100000083, 49, 41, 50, 51, 86, 100000087, 51, 87, 52, 53, 50, 100000091, 53, 52, 54, 100000036, 89, 100000095,
                                                  100000036, 12, 56, 147, 37, 162, 57, 26, 58, 254, 82, 59, 60, 31, 68, 60, 84, 61, 62, 40, 100000039, 62, 85, 63, 64,
                                                  42, 100000047, 64, 43, 65, 66, 86, 100000051, 66, 51, 67, 100000036, 88, 100000055, 100000036, 84, 69, 100000043, 40,
                                                  100000039, 100000043, 16, 77, 71, 47, 146, 72, 53, 134, 73, 59, 74, 79, 10, 897, 75, 12, 107, 76, 36, 82, 897, 47,
                                                  146, 78, 53, 134, 79, 10, 118, 80, 12, 107, 81, 36, 82, 119, 17, 83, 87, 18, 84, 87, 23, 85, 87, 20, 86, 87, 24,
                                                  1012, 87, 26, 88, 323, 34, 89, 90, 39, 974, 90, 45, 967, 91, 46, 92, 100000171, 48, 93, 976, 49, 100000171, 94, 54,
                                                  984, 95, 55, 96, 950, 56, 988, 97, 60, 98, 100000143, 63, 100000143, 99, 62, 100, 102, 64, 101, 102, 65, 102,
                                                  100000132, 66, 103, 100000140, 69, 104, 100000139, 68, 105, 100000151, 76, 106, 100000138, 73, 100000148, 100000137,
                                                  17, 108, 112, 18, 109, 112, 23, 110, 112, 20, 111, 112, 24, 1012, 112, 32, 113, 100000128, 26, 117, 114, 78,
                                                  100000125, 115, 79, 116, 100000127, 80, 100000126, 100000127, 78, 100000117, 100000118, 11, 1007, 119, 17, 120, 124,
                                                  18, 121, 124, 23, 122, 124, 20, 123, 124, 24, 1012, 124, 37, 129, 125, 19, 126, 100000200, 21, 127, 100000200, 28,
                                                  128, 100000128, 26, 100000129, 100000188, 26, 133, 130, 78, 100000182, 131, 79, 132, 100000184, 80, 100000183,
                                                  100000184, 78, 100000178, 100000179, 12, 135, 100000009, 26, 136, 536, 27, 137, 138, 82, 100000059, 138, 83, 139,
                                                  140, 33, 100000063, 140, 41, 141, 142, 86, 100000067, 142, 87, 143, 144, 50, 100000071, 144, 52, 145, 100000036, 89,
                                                  100000075, 100000036, 12, 148, 147, 37, 162, 100000009, 37, 162, 149, 26, 150, 549, 82, 151, 152, 31, 160, 152, 84,
                                                  153, 154, 40, 100000018, 154, 85, 155, 156, 42, 100000026, 156, 43, 157, 158, 86, 100000030, 158, 51, 159, 100000036,
                                                  88, 100000034, 100000036, 84, 161, 100000022, 40, 100000018, 100000022, 26, 100000014, 100000015, 2, 178, 164, 15,
                                                  165, 515, 3, 166, 167, 8, 175, 167, 38, 170, 168, 19, 169, 517, 21, 390, 517, 19, 171, 172, 21, 516, 172, 28, 173,
                                                  517, 26, 100000002, 174, 78, 100000195, 100000196, 38, 177, 176, 26, 100000002, 100000101, 26, 100000002, 100000097,
                                                  15, 231, 179, 7, 180, 181, 9, 187, 181, 16, 184, 182, 47, 186, 183, 53, 186, 190, 47, 186, 185, 53, 186, 196, 38,
                                                  266, 564, 16, 194, 188, 47, 230, 189, 53, 230, 190, 59, 191, 196, 90, 192, 312, 10, 297, 193, 36, 199, 297, 47, 230,
                                                  195, 53, 230, 196, 90, 197, 312, 10, 229, 198, 36, 199, 515, 26, 100000002, 200, 34, 201, 202, 39, 217, 202, 44, 208,
                                                  203, 45, 206, 204, 46, 205, 100000171, 48, 211, 220, 46, 207, 100000171, 48, 215, 220, 45, 213, 209, 46, 210,
                                                  100000171, 48, 211, 475, 49, 100000171, 212, 54, 225, 951, 46, 214, 100000171, 48, 215, 475, 49, 100000171, 216, 54,
                                                  225, 972, 44, 221, 218, 46, 219, 100000171, 48, 223, 220, 49, 100000171, 453, 46, 222, 100000171, 48, 223, 475, 49,
                                                  100000171, 224, 54, 225, 982, 56, 732, 226, 60, 227, 100000143, 63, 100000143, 228, 74, 486, 100000158, 11, 526, 515,
                                                  38, 561, 564, 7, 232, 233, 9, 273, 233, 16, 236, 234, 47, 251, 235, 53, 238, 276, 47, 251, 237, 53, 238, 300, 38,
                                                  266, 239, 12, 240, 564, 26, 100000002, 241, 27, 242, 243, 82, 100000080, 243, 83, 244, 245, 33, 100000084, 245, 41,
                                                  246, 247, 86, 100000088, 247, 87, 248, 249, 50, 100000092, 249, 52, 250, 100000036, 89, 100000096, 100000036, 38,
                                                  266, 252, 12, 253, 564, 26, 100000002, 254, 82, 255, 256, 31, 264, 256, 84, 257, 258, 40, 100000040, 258, 85, 259,
                                                  260, 42, 100000048, 260, 43, 261, 262, 86, 100000052, 262, 51, 263, 100000036, 88, 100000056, 100000036, 84, 265,
                                                  100000044, 40, 100000040, 100000044, 12, 269, 267, 37, 268, 564, 25, 272, 564, 37, 270, 271, 25, 272, 271, 26,
                                                  100000002, 100000011, 26, 100000002, 100000010, 16, 298, 274, 47, 546, 275, 53, 533, 276, 59, 277, 300, 90, 278, 301,
                                                  38, 287, 279, 10, 282, 280, 12, 376, 281, 36, 317, 282, 17, 283, 297, 18, 284, 297, 23, 285, 297, 20, 286, 297, 24,
                                                  528, 297, 10, 292, 288, 12, 494, 289, 13, 290, 291, 14, 487, 291, 36, 403, 292, 17, 293, 297, 18, 294, 297, 23, 295,
                                                  297, 20, 296, 297, 24, 527, 297, 26, 100000002, 100000172, 47, 546, 299, 53, 533, 300, 90, 313, 301, 38, 307, 302,
                                                  17, 303, 312, 18, 304, 312, 23, 305, 312, 20, 306, 312, 24, 528, 312, 17, 308, 312, 18, 309, 312, 23, 310, 312, 20,
                                                  311, 312, 24, 527, 312, 26, 100000002, 100000109, 38, 398, 314, 10, 381, 315, 12, 376, 316, 36, 317, 382, 17, 318,
                                                  322, 18, 319, 322, 23, 320, 322, 20, 321, 322, 24, 528, 322, 26, 100000002, 323, 34, 324, 325, 39, 358, 325, 44, 331,
                                                  326, 45, 329, 327, 46, 328, 100000171, 48, 334, 361, 46, 330, 100000171, 48, 446, 361, 45, 444, 332, 46, 333,
                                                  100000171, 48, 334, 475, 49, 100000171, 335, 54, 480, 336, 55, 337, 951, 56, 348, 338, 60, 339, 100000143, 63,
                                                  100000143, 340, 62, 341, 343, 64, 342, 343, 65, 343, 100000132, 66, 344, 100000140, 69, 345, 100000139, 68, 346,
                                                  100000151, 76, 347, 100000138, 73, 100000150, 100000137, 60, 349, 100000143, 63, 100000143, 350, 62, 351, 353, 64,
                                                  352, 353, 65, 353, 100000132, 66, 354, 100000140, 69, 355, 100000139, 67, 100000133, 356, 76, 357, 100000138, 73,
                                                  100000136, 100000137, 44, 473, 359, 46, 360, 100000171, 48, 478, 361, 49, 100000171, 362, 55, 363, 453, 56,
                                                  100000144, 364, 57, 365, 100000170, 58, 366, 100000169, 61, 367, 100000168, 62, 368, 370, 64, 369, 370, 65, 370,
                                                  100000132, 66, 371, 100000140, 69, 372, 100000139, 76, 373, 100000138, 70, 374, 100000167, 71, 375, 100000166, 72,
                                                  100000164, 100000165, 17, 377, 517, 18, 378, 517, 23, 379, 517, 20, 380, 517, 24, 528, 517, 11, 393, 382, 17, 383,
                                                  387, 18, 384, 387, 23, 385, 387, 20, 386, 387, 24, 528, 387, 37, 392, 388, 19, 389, 515, 21, 390, 515, 28, 391, 517,
                                                  26, 100000002, 100000188, 26, 100000002, 100000185, 17, 394, 526, 18, 395, 526, 23, 396, 526, 20, 397, 526, 24, 528,
                                                  526, 10, 506, 399, 12, 494, 400, 13, 401, 402, 14, 487, 402, 36, 403, 507, 17, 404, 408, 18, 405, 408, 23, 406, 408,
                                                  20, 407, 408, 24, 527, 408, 26, 100000002, 409, 34, 410, 411, 39, 448, 411, 44, 417, 412, 45, 415, 413, 46, 414,
                                                  100000171, 48, 420, 451, 46, 416, 100000171, 48, 446, 451, 45, 444, 418, 46, 419, 100000171, 48, 420, 475, 49,
                                                  100000171, 421, 54, 480, 422, 55, 423, 951, 56, 434, 424, 60, 425, 100000143, 63, 100000143, 426, 62, 427, 429, 64,
                                                  428, 429, 65, 429, 100000132, 66, 430, 100000140, 69, 431, 100000139, 68, 432, 100000151, 76, 433, 100000138, 73,
                                                  100000149, 100000137, 60, 435, 100000143, 63, 100000143, 436, 62, 437, 439, 64, 438, 439, 65, 439, 100000132, 66,
                                                  440, 100000140, 69, 441, 100000139, 67, 100000133, 442, 76, 443, 100000138, 73, 100000134, 100000137, 46, 445,
                                                  100000171, 48, 446, 475, 49, 100000171, 447, 54, 480, 972, 44, 473, 449, 46, 450, 100000171, 48, 478, 451, 49,
                                                  100000171, 452, 55, 460, 453, 56, 100000144, 454, 57, 455, 100000170, 58, 456, 100000169, 61, 457, 100000168, 62,
                                                  458, 100000168, 64, 459, 100000168, 65, 100000168, 100000132, 56, 100000144, 461, 57, 462, 100000170, 58, 463,
                                                  100000169, 61, 464, 100000168, 62, 465, 467, 64, 466, 467, 65, 467, 100000132, 66, 468, 100000140, 69, 469,
                                                  100000139, 76, 470, 100000138, 70, 471, 100000167, 71, 472, 100000166, 72, 100000163, 100000165, 46, 474, 100000171,
                                                  48, 478, 475, 49, 100000171, 476, 56, 100000144, 477, 57, 100000162, 100000170, 49, 100000171, 479, 54, 480, 982, 56,
                                                  732, 481, 60, 482, 100000143, 63, 100000143, 483, 74, 486, 484, 75, 485, 100000158, 81, 100000156, 100000157, 75,
                                                  100000155, 100000158, 17, 488, 492, 18, 489, 492, 23, 490, 492, 20, 491, 492, 24, 527, 492, 32, 493, 517, 26,
                                                  100000002, 100000122, 17, 495, 499, 18, 496, 499, 23, 497, 499, 20, 498, 499, 24, 527, 499, 32, 500, 517, 25, 504,
                                                  501, 26, 100000002, 502, 77, 503, 100000128, 78, 100000120, 100000122, 26, 100000002, 505, 78, 100000119, 100000121,
                                                  11, 521, 507, 17, 508, 512, 18, 509, 512, 23, 510, 512, 20, 511, 512, 24, 527, 512, 37, 519, 513, 19, 514, 515, 21,
                                                  516, 515, 26, 100000002, 100000200, 28, 518, 517, 26, 100000002, 100000128, 26, 100000002, 100000186, 26, 100000002,
                                                  520, 78, 100000180, 100000181, 17, 522, 526, 18, 523, 526, 23, 524, 526, 20, 525, 526, 24, 527, 526, 26, 100000002,
                                                  100000173, 22, 529, 528, 26, 100000002, 1013, 26, 100000002, 530, 35, 531, 100000108, 29, 100000103, 532, 30,
                                                  100000105, 100000107, 38, 561, 534, 12, 535, 564, 26, 100000002, 536, 27, 537, 538, 82, 100000060, 538, 83, 539, 540,
                                                  33, 100000064, 540, 41, 541, 542, 86, 100000068, 542, 87, 543, 544, 50, 100000072, 544, 52, 545, 100000036, 89,
                                                  100000076, 100000036, 38, 561, 547, 12, 548, 564, 26, 100000002, 549, 82, 550, 551, 31, 559, 551, 84, 552, 553, 40,
                                                  100000019, 553, 85, 554, 555, 42, 100000027, 555, 43, 556, 557, 86, 100000031, 557, 51, 558, 100000036, 88,
                                                  100000035, 100000036, 84, 560, 100000023, 40, 100000019, 100000023, 12, 565, 562, 37, 563, 564, 25, 568, 564, 26,
                                                  100000002, 100000009, 37, 566, 567, 25, 568, 567, 26, 100000002, 100000008, 26, 100000002, 100000007, 1, 827, 570, 2,
                                                  582, 571, 15, 572, 100000200, 6, 100000005, 573, 3, 574, 575, 8, 581, 575, 19, 576, 577, 21, 771, 577, 28, 578,
                                                  100000128, 26, 580, 579, 78, 100000191, 100000192, 78, 100000189, 100000190, 26, 100000098, 100000099, 15, 601, 583,
                                                  16, 589, 584, 47, 100000009, 585, 53, 100000009, 586, 59, 587, 591, 10, 100000172, 588, 36, 593, 100000172, 47,
                                                  100000009, 590, 53, 100000009, 591, 10, 869, 592, 36, 593, 100000200, 26, 862, 594, 34, 595, 596, 39, 721, 596, 45,
                                                  717, 597, 46, 598, 100000171, 48, 599, 723, 49, 100000171, 600, 54, 728, 951, 6, 100000005, 602, 3, 603, 604, 8, 614,
                                                  604, 7, 605, 606, 9, 610, 606, 16, 608, 607, 47, 609, 618, 47, 609, 620, 12, 645, 100000009, 16, 612, 611, 47, 613,
                                                  672, 47, 613, 678, 12, 801, 100000009, 7, 615, 616, 9, 670, 616, 16, 619, 617, 47, 643, 618, 53, 621, 673, 47, 643,
                                                  620, 53, 621, 679, 12, 622, 100000009, 26, 633, 623, 27, 624, 625, 82, 100000078, 625, 83, 626, 627, 33, 100000082,
                                                  627, 41, 628, 629, 86, 100000086, 629, 87, 630, 631, 50, 100000090, 631, 52, 632, 100000036, 89, 100000094,
                                                  100000036, 27, 634, 635, 82, 100000077, 635, 83, 636, 637, 33, 100000081, 637, 41, 638, 639, 86, 100000085, 639, 87,
                                                  640, 641, 50, 100000089, 641, 52, 642, 100000036, 89, 100000093, 100000036, 12, 644, 799, 37, 826, 645, 26, 658, 646,
                                                  82, 647, 648, 31, 656, 648, 84, 649, 650, 40, 100000038, 650, 85, 651, 652, 42, 100000046, 652, 43, 653, 654, 86,
                                                  100000050, 654, 51, 655, 100000036, 88, 100000054, 100000036, 84, 657, 100000042, 40, 100000038, 100000042, 82, 659,
                                                  660, 31, 668, 660, 84, 661, 662, 40, 100000037, 662, 85, 663, 664, 42, 100000045, 664, 43, 665, 666, 86, 100000049,
                                                  666, 51, 667, 100000036, 88, 100000053, 100000036, 84, 669, 100000041, 40, 100000037, 100000041, 16, 677, 671, 47,
                                                  798, 672, 53, 776, 673, 59, 674, 679, 10, 897, 675, 12, 753, 676, 36, 682, 897, 47, 798, 678, 53, 776, 679, 10, 762,
                                                  680, 12, 753, 681, 36, 682, 763, 17, 683, 687, 18, 684, 687, 23, 685, 687, 20, 686, 687, 24, 1012, 687, 26, 734, 688,
                                                  34, 689, 690, 39, 721, 690, 45, 717, 691, 46, 692, 100000171, 48, 693, 723, 49, 100000171, 694, 54, 728, 695, 55,
                                                  696, 951, 56, 707, 697, 60, 698, 100000143, 63, 100000143, 699, 62, 700, 702, 64, 701, 702, 65, 702, 100000132, 66,
                                                  703, 100000140, 69, 704, 100000139, 68, 705, 100000151, 76, 706, 100000138, 73, 100000147, 100000137, 60, 708,
                                                  100000143, 63, 100000143, 709, 62, 710, 712, 64, 711, 712, 65, 712, 100000132, 66, 713, 100000140, 69, 714,
                                                  100000139, 67, 100000133, 715, 76, 716, 100000138, 73, 100000135, 100000137, 46, 718, 100000171, 48, 719, 723, 49,
                                                  100000171, 720, 54, 728, 972, 46, 722, 100000171, 48, 726, 723, 49, 100000171, 724, 56, 100000144, 725, 57,
                                                  100000160, 100000170, 49, 100000171, 727, 54, 728, 982, 56, 732, 729, 60, 730, 100000143, 63, 100000143, 731, 75,
                                                  100000153, 100000158, 60, 733, 100000143, 63, 100000143, 100000142, 34, 735, 736, 39, 974, 736, 45, 967, 737, 46,
                                                  738, 100000171, 48, 739, 976, 49, 100000171, 740, 54, 984, 741, 55, 742, 950, 56, 988, 743, 60, 744, 100000143, 63,
                                                  100000143, 745, 62, 746, 748, 64, 747, 748, 65, 748, 100000132, 66, 749, 100000140, 69, 750, 100000139, 68, 751,
                                                  100000151, 76, 752, 100000138, 73, 100000146, 100000137, 17, 754, 758, 18, 755, 758, 23, 756, 758, 20, 757, 758, 24,
                                                  1012, 758, 32, 759, 100000128, 26, 761, 760, 78, 100000113, 100000114, 78, 100000111, 100000112, 11, 1007, 763, 17,
                                                  764, 768, 18, 765, 768, 23, 766, 768, 20, 767, 768, 24, 1012, 768, 37, 773, 769, 19, 770, 100000200, 21, 771,
                                                  100000200, 28, 772, 100000128, 26, 100000129, 100000187, 26, 775, 774, 78, 100000176, 100000177, 78, 100000174,
                                                  100000175, 12, 777, 100000009, 26, 788, 778, 27, 779, 780, 82, 100000058, 780, 83, 781, 782, 33, 100000062, 782, 41,
                                                  783, 784, 86, 100000066, 784, 87, 785, 786, 50, 100000070, 786, 52, 787, 100000036, 89, 100000074, 100000036, 27,
                                                  789, 790, 82, 100000057, 790, 83, 791, 792, 33, 100000061, 792, 41, 793, 794, 86, 100000065, 794, 87, 795, 796, 50,
                                                  100000069, 796, 52, 797, 100000036, 89, 100000073, 100000036, 12, 800, 799, 37, 826, 100000009, 37, 826, 801, 26,
                                                  814, 802, 82, 803, 804, 31, 812, 804, 84, 805, 806, 40, 100000017, 806, 85, 807, 808, 42, 100000025, 808, 43, 809,
                                                  810, 86, 100000029, 810, 51, 811, 100000036, 88, 100000033, 100000036, 84, 813, 100000021, 40, 100000017, 100000021,
                                                  82, 815, 816, 31, 824, 816, 84, 817, 818, 40, 100000016, 818, 85, 819, 820, 42, 100000024, 820, 43, 821, 822, 86,
                                                  100000028, 822, 51, 823, 100000036, 88, 100000032, 100000036, 84, 825, 100000020, 40, 100000016, 100000020, 26,
                                                  100000012, 100000013, 26, 100000002, 100000003, 5, 100000001, 829, 1, 1019, 830, 2, 843, 831, 15, 832, 100000200, 3,
                                                  833, 834, 8, 842, 834, 19, 835, 836, 21, 1005, 836, 28, 837, 100000128, 26, 841, 838, 78, 100000197, 839, 79, 840,
                                                  100000199, 80, 100000198, 100000199, 78, 100000193, 100000194, 26, 100000100, 100000101, 15, 870, 844, 16, 850, 845,
                                                  47, 100000006, 846, 53, 100000006, 847, 59, 848, 852, 10, 100000172, 849, 36, 854, 100000172, 47, 100000006, 851, 53,
                                                  100000006, 852, 10, 869, 853, 36, 854, 100000200, 26, 862, 855, 34, 856, 857, 39, 928, 857, 45, 923, 858, 46, 859,
                                                  100000171, 48, 860, 930, 49, 100000171, 861, 54, 936, 921, 34, 863, 864, 39, 974, 864, 45, 967, 865, 46, 866,
                                                  100000171, 48, 867, 976, 49, 100000171, 868, 54, 984, 950, 11, 100000173, 100000200, 6, 890, 871, 16, 877, 872, 47,
                                                  100000006, 873, 53, 100000006, 874, 59, 875, 879, 10, 897, 876, 12, 881, 896, 47, 100000006, 878, 53, 100000006, 879,
                                                  10, 996, 880, 12, 881, 906, 17, 882, 886, 18, 883, 886, 23, 884, 886, 20, 885, 886, 24, 1012, 886, 32, 887,
                                                  100000128, 26, 889, 888, 78, 100000123, 100000124, 78, 100000115, 100000116, 16, 902, 891, 47, 100000006, 892, 53,
                                                  100000006, 893, 59, 894, 904, 10, 897, 895, 12, 990, 896, 36, 907, 897, 17, 898, 100000172, 18, 899, 100000172, 23,
                                                  900, 100000172, 20, 901, 100000172, 24, 1012, 100000172, 47, 100000006, 903, 53, 100000006, 904, 10, 996, 905, 12,
                                                  990, 906, 36, 907, 997, 17, 908, 912, 18, 909, 912, 23, 910, 912, 20, 911, 912, 24, 1012, 912, 26, 942, 913, 34, 914,
                                                  915, 39, 928, 915, 45, 923, 916, 46, 917, 100000171, 48, 918, 930, 49, 100000171, 919, 54, 936, 920, 55, 922, 921,
                                                  56, 940, 951, 56, 940, 957, 46, 924, 100000171, 48, 925, 930, 49, 100000171, 926, 54, 936, 927, 56, 940, 972, 46,
                                                  929, 100000171, 48, 933, 930, 49, 100000171, 931, 56, 100000144, 932, 57, 100000161, 100000170, 49, 100000171, 934,
                                                  54, 936, 935, 56, 940, 982, 56, 940, 937, 60, 938, 100000143, 63, 100000143, 939, 75, 100000154, 100000158, 60, 941,
                                                  100000143, 63, 100000143, 100000130, 34, 943, 944, 39, 974, 944, 45, 967, 945, 46, 946, 100000171, 48, 947, 976, 49,
                                                  100000171, 948, 54, 984, 949, 55, 956, 950, 56, 988, 951, 60, 952, 100000143, 63, 100000143, 953, 62, 954, 100000141,
                                                  64, 955, 100000141, 65, 100000141, 100000132, 56, 988, 957, 60, 958, 100000143, 63, 100000143, 959, 62, 960, 962, 64,
                                                  961, 962, 65, 962, 100000132, 66, 963, 100000140, 69, 964, 100000139, 68, 965, 100000151, 76, 966, 100000138, 73,
                                                  100000145, 100000137, 46, 968, 100000171, 48, 969, 976, 49, 100000171, 970, 54, 984, 971, 56, 988, 972, 60, 973,
                                                  100000143, 63, 100000143, 100000141, 46, 975, 100000171, 48, 979, 976, 49, 100000171, 977, 56, 100000144, 978, 57,
                                                  100000159, 100000170, 49, 100000171, 980, 54, 984, 981, 56, 988, 982, 60, 983, 100000143, 63, 100000143, 100000131,
                                                  56, 988, 985, 60, 986, 100000143, 63, 100000143, 987, 75, 100000152, 100000158, 60, 989, 100000143, 63, 100000143,
                                                  100000129, 17, 991, 995, 18, 992, 995, 23, 993, 995, 20, 994, 995, 24, 1012, 995, 32, 100000110, 100000128, 11, 1007,
                                                  997, 17, 998, 1002, 18, 999, 1002, 23, 1000, 1002, 20, 1001, 1002, 24, 1012, 1002, 37, 100000185, 1003, 19, 1004,
                                                  100000200, 21, 1005, 100000200, 28, 1006, 100000128, 26, 100000129, 100000130, 17, 1008, 100000173, 18, 1009,
                                                  100000173, 23, 1010, 100000173, 20, 1011, 100000173, 24, 1012, 100000173, 22, 1016, 1013, 35, 1014, 100000108, 29,
                                                  100000104, 1015, 30, 100000106, 100000107, 35, 1017, 100000108, 29, 100000102, 1018, 30, 100000102, 100000107, 26,
                                                  100000002, 100000004 };

    private static final ConditionFn[] CONDITION_FNS = {
        // condition 0
        (registers) -> {
            return (registers.region != null);
        }, // condition 1
        (registers) -> {
            return (registers.endpoint != null);
        }, // condition 2
        (registers) -> {
            return (registers.bucket != null);
        }, // condition 3
        (registers) -> {
            return (registers.useS3ExpressControlEndpoint != null);
        }, // condition 4
        (registers) -> {
            return (registers.accelerate);
        }, // condition 5
        (registers) -> {
            return (registers.useFIPS);
        }, // condition 6
        (registers) -> {
            return ("aws-cn".equals(registers.partitionResult.name()));
        }, // condition 7
        (registers) -> {
            return (registers.disableS3ExpressSessionAuth != null);
        }, // condition 8
        (registers) -> {
            return (Boolean.FALSE != registers.useS3ExpressControlEndpoint);
        }, // condition 9
        (registers) -> {
            return (Boolean.FALSE != registers.disableS3ExpressSessionAuth);
        }, // condition 10
        (registers) -> {
            return (registers.forcePathStyle);
        }, // condition 11
        (registers) -> {
            return (RulesFunctions.awsParseArn(registers.bucket) != null);
        }, // condition 12
        (registers) -> {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, false));
        }, // condition 13
        (registers) -> {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, true));
        }, // condition 14
        (registers) -> {
            return ("http".equals(registers.url.scheme()));
        }, // condition 15, assign partitionResult
        (registers) -> {
            registers.partitionResult = RulesFunctions.awsPartition(registers.region);
            return registers.partitionResult != null;
        }, // condition 16
        (registers) -> {
            return (RulesFunctions.awsParseArn(registers.bucket) != null);
        }, // condition 17, assign outpostId_ssa_2
        (registers) -> {
            registers.outpostId_ssa_2 = RulesFunctions.substring(registers.bucket, 32, 49, true);
            return registers.outpostId_ssa_2 != null;
        }, // condition 18, assign hardwareType
        (registers) -> {
            registers.hardwareType = RulesFunctions.substring(registers.bucket, 49, 50, true);
            return registers.hardwareType != null;
        }, // condition 19
        (registers) -> {
            return (registers.useObjectLambdaEndpoint != null);
        }, // condition 20
        (registers) -> {
            return ("--op-s3".equals(registers.accessPointSuffix));
        }, // condition 21
        (registers) -> {
            return (Boolean.FALSE != registers.useObjectLambdaEndpoint);
        }, // condition 22
        (registers) -> {
            return ("beta".equals(registers.regionPrefix));
        }, // condition 23, assign accessPointSuffix
        (registers) -> {
            registers.accessPointSuffix = RulesFunctions.substring(registers.bucket, 0, 7, true);
            return registers.accessPointSuffix != null;
        }, // condition 24, assign regionPrefix
        (registers) -> {
            registers.regionPrefix = RulesFunctions.substring(registers.bucket, 8, 12, true);
            return registers.regionPrefix != null;
        }, // condition 25
        (registers) -> {
            return (registers.url.isIp());
        }, // condition 26
        (registers) -> {
            return (registers.useDualStack);
        }, // condition 27, assign s3expressAvailabilityZoneId_ssa_6
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
            return registers.s3expressAvailabilityZoneId_ssa_6 != null;
        }, // condition 28
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.region, true));
        }, // condition 29
        (registers) -> {
            return ("e".equals(registers.hardwareType));
        }, // condition 30
        (registers) -> {
            return ("o".equals(registers.hardwareType));
        }, // condition 31, assign s3expressAvailabilityZoneId_ssa_2
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
            return registers.s3expressAvailabilityZoneId_ssa_2 != null;
        }, // condition 32
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.region, false));
        }, // condition 33, assign s3expressAvailabilityZoneId_ssa_7
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
            return registers.s3expressAvailabilityZoneId_ssa_7 != null;
        }, // condition 34
        (registers) -> {
            return (registers.disableAccessPoints != null);
        }, // condition 35
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
        }, // condition 36, assign bucketArn
        (registers) -> {
            registers.bucketArn = RulesFunctions.awsParseArn(registers.bucket);
            return registers.bucketArn != null;
        }, // condition 37, assign uri_encoded_bucket
        (registers) -> {
            registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
            return registers.uri_encoded_bucket != null;
        }, // condition 38, assign url
        (registers) -> {
            registers.url = RulesFunctions.parseURL(registers.endpoint);
            return registers.url != null;
        }, // condition 39
        (registers) -> {
            return (Boolean.FALSE != registers.disableAccessPoints);
        }, // condition 40, assign s3expressAvailabilityZoneId_ssa_1
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
            return registers.s3expressAvailabilityZoneId_ssa_1 != null;
        }, // condition 41, assign s3expressAvailabilityZoneId_ssa_8
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
            return registers.s3expressAvailabilityZoneId_ssa_8 != null;
        }, // condition 42, assign s3expressAvailabilityZoneId_ssa_3
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
            return registers.s3expressAvailabilityZoneId_ssa_3 != null;
        }, // condition 43, assign s3expressAvailabilityZoneId_ssa_4
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
            return registers.s3expressAvailabilityZoneId_ssa_4 != null;
        }, // condition 44
        (registers) -> {
            return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
        }, // condition 45
        (registers) -> {
            return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
        }, // condition 46, assign arnType
        (registers) -> {
            registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
            return registers.arnType != null;
        }, // condition 47
        (registers) -> {
            return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 6, true), "")));
        }, // condition 48
        (registers) -> {
            return ("accesspoint".equals(registers.arnType));
        }, // condition 49
        (registers) -> {
            return ("".equals(registers.arnType));
        }, // condition 50, assign s3expressAvailabilityZoneId_ssa_9
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
            return registers.s3expressAvailabilityZoneId_ssa_9 != null;
        }, // condition 51, assign s3expressAvailabilityZoneId_ssa_5
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
            return registers.s3expressAvailabilityZoneId_ssa_5 != null;
        }, // condition 52, assign s3expressAvailabilityZoneId_ssa_10
        (registers) -> {
            registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
            return registers.s3expressAvailabilityZoneId_ssa_10 != null;
        }, // condition 53
        (registers) -> {
            return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 7, true), "")));
        }, // condition 54
        (registers) -> {
            return ("".equals(registers.bucketArn.region()));
        }, // condition 55, assign bucketPartition
        (registers) -> {
            registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
            return registers.bucketPartition != null;
        }, // condition 56
        (registers) -> {
            return ("s3-object-lambda".equals(registers.bucketArn.service()));
        }, // condition 57
        (registers) -> {
            return ("s3-outposts".equals(registers.bucketArn.service()));
        }, // condition 58, assign outpostId_ssa_1
        (registers) -> {
            registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
            return registers.outpostId_ssa_1 != null;
        }, // condition 59
        (registers) -> {
            return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), "")));
        }, // condition 60, assign accessPointName_ssa_1
        (registers) -> {
            registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
            return registers.accessPointName_ssa_1 != null;
        }, // condition 61
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
        }, // condition 62
        (registers) -> {
            return (registers.useArnRegion != null);
        }, // condition 63
        (registers) -> {
            return ("".equals(registers.accessPointName_ssa_1));
        }, // condition 64
        (registers) -> {
            return (!registers.useArnRegion);
        }, // condition 65
        (registers) -> {
            return (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region()));
        }, // condition 66
        (registers) -> {
            return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
        }, // condition 67
        (registers) -> {
            return ("".equals(registers.bucketArn.accountId()));
        }, // condition 68
        (registers) -> {
            return ("s3".equals(registers.bucketArn.service()));
        }, // condition 69
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
        }, // condition 70, assign outpostType
        (registers) -> {
            registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
            return registers.outpostType != null;
        }, // condition 71, assign accessPointName_ssa_2
        (registers) -> {
            registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
            return registers.accessPointName_ssa_2 != null;
        }, // condition 72
        (registers) -> {
            return ("accesspoint".equals(registers.outpostType));
        }, // condition 73
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
        }, // condition 74
        (registers) -> {
            return (registers.disableMultiRegionAccessPoints);
        }, // condition 75
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
        }, // condition 76
        (registers) -> {
            return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
        }, // condition 77
        (registers) -> {
            return (!registers.url.isIp());
        }, // condition 78
        (registers) -> {
            return ("aws-global".equals(registers.region));
        }, // condition 79
        (registers) -> {
            return ("us-east-1".equals(registers.region));
        }, // condition 80
        (registers) -> {
            return (registers.useGlobalEndpoint);
        }, // condition 81
        (registers) -> {
            return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
        }, // condition 82
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), "")));
        }, // condition 83
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), "")));
        }, // condition 84
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), "")));
        }, // condition 85
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), "")));
        }, // condition 86
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), "")));
        }, // condition 87
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), "")));
        }, // condition 88
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), "")));
        }, // condition 89
        (registers) -> {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), "")));
        }, // condition 90
        (registers) -> {
            return (RulesFunctions.parseURL(registers.endpoint) != null);
        }

    };

    private static final ResultFn[] RESULT_FNS = {
        (registers) -> {
            return RuleResult.error("Accelerate cannot be used with FIPS");
        },
        (registers) -> {
            return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
        },
        (registers) -> {
            return RuleResult.error("A custom endpoint cannot be combined with FIPS");
        },
        (registers) -> {
            return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.error("Partition does not support FIPS");
        },
        (registers) -> {
            return RuleResult.error("S3Express does not support S3 Accelerate.");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/"
                                                           + registers.uri_encoded_bucket + registers.url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                           + registers.url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/"
                                                           + registers.uri_encoded_bucket + registers.url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                           + registers.url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-fips-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3express-"
                                                           + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                        .signingName("s3express").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control-fips.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control-fips." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3express-control." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".ec2." + registers.url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.region)
                                                                                                                                           .build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".ec2.s3-outposts." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.region)
                                                                                                                                           .build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + "."
                                                           + registers.url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.region)
                                                                                                                                           .build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".op-" + registers.outpostId_ssa_2 + ".s3-outposts."
                                                           + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                           .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.region)
                                                                                                                                           .build())).build());
        },
        (registers) -> {
            return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got "
                                    + registers.hardwareType + "\"");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
        },
        (registers) -> {
            return RuleResult.error("Custom endpoint `" + registers.endpoint + "` was not a valid URI");
        },
        (registers) -> {
            return RuleResult.error("S3 Accelerate cannot be used in this region");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack.us-east-1."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-fips.us-east-1."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-fips." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack.us-east-1."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3.dualstack.us-east-1."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority()
                                                           + registers.url.normalizedPath() + registers.bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                           + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority()
                                                           + registers.url.normalizedPath() + registers.bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                           + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + registers.bucket + ".s3-accelerate."
                                              + registers.partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + registers.bucket + ".s3-accelerate."
                                              + registers.partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.bucket + ".s3." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        },
        (registers) -> {
            return RuleResult.error("S3 Object Lambda does not support Dual-stack");
        },
        (registers) -> {
            return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.error("Access points are not supported for this operation");
        },
        (registers) -> {
            return RuleResult.error("Invalid configuration: region from ARN `" + registers.bucketArn.region()
                                    + "` does not match client region `" + registers.region + "` and UseArnRegion is `false`");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: Missing account id");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                           + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.bucketArn.region()).build()))
                                           .build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                           + ".s3-object-lambda-fips." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.bucketArn.region()).build()))
                                           .build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                           + ".s3-object-lambda." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.bucketArn.region()).build()))
                                           .build());
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + registers.accessPointName_ssa_1 + "`");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + registers.bucketArn.accountId() + "`");
        },
        (registers) -> {
            return RuleResult.error("Invalid region in ARN: `" + registers.bucketArn.region() + "` (invalid DNS name)");
        },
        (registers) -> {
            return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                    + "` but ARN (`" + registers.bucket + "`) has `" + registers.bucketPartition.name() + "`");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
        },
        (registers) -> {
            return RuleResult
                .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                    + registers.arnType + "`");
        },
        (registers) -> {
            return RuleResult.error("Access Points do not support S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                           + ".s3-accesspoint-fips.dualstack." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                           + ".s3-accesspoint-fips." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                           + ".s3-accesspoint.dualstack." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                           + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-"
                                              + registers.bucketArn.accountId() + ".s3-accesspoint." + registers.bucketArn.region()
                                              + "." + registers.bucketPartition.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: "
                                    + registers.bucketArn.service());
        },
        (registers) -> {
            return RuleResult.error("S3 MRAP does not support dual-stack");
        },
        (registers) -> {
            return RuleResult.error("S3 MRAP does not support FIPS");
        },
        (registers) -> {
            return RuleResult.error("S3 MRAP does not support S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                             .signingRegionSet(Arrays.asList("*")).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                    + "` but bucket referred to partition `" + registers.bucketArn.partition() + "`");
        },
        (registers) -> {
            return RuleResult.error("Invalid Access Point Name");
        },
        (registers) -> {
            return RuleResult.error("S3 Outposts does not support Dual-stack");
        },
        (registers) -> {
            return RuleResult.error("S3 Outposts does not support FIPS");
        },
        (registers) -> {
            return RuleResult.error("S3 Outposts does not support S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId()
                                                           + "." + registers.outpostId_ssa_1 + "." + registers.url.authority()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(),
                                                             SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId()
                                                           + "." + registers.outpostId_ssa_1 + ".s3-outposts." + registers.bucketArn.region() + "."
                                                           + registers.bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                             .signingRegionSet(Arrays.asList("*")).build(),
                                                             SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                            .signingRegion(registers.bucketArn.region()).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Expected an outpost type `accesspoint`, found " + registers.outpostType);
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: expected an access point name");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: Expected a 4-component resource");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + registers.outpostId_ssa_1 + "`");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: The Outpost Id was not set");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: Unrecognized format: " + registers.bucket + " (type: " + registers.arnType
                                    + ")");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: No ARN type specified");
        },
        (registers) -> {
            return RuleResult.error("Invalid ARN: `" + registers.bucket + "` was not a valid ARN");
        },
        (registers) -> {
            return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                           + "/" + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority()
                                                           + registers.url.normalizedPath() + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority()
                                                           + registers.url.normalizedPath() + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                           + registers.uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-object-lambda-fips." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-object-lambda." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                            .signingName("s3-object-lambda").signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + registers.region + "."
                                                           + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult
                .endpoint(Endpoint
                              .builder()
                              .url(URI.create("https://s3.dualstack." + registers.region + "."
                                              + registers.partitionResult.dnsSuffix()))
                              .putAttribute(
                                  AwsEndpointAttribute.AUTH_SCHEMES,
                                  Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                               .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        },
        (registers) -> {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(registers.region).build())).build());
        }, (registers) -> {
        return RuleResult.error("A region must be set when sending requests to S3.");
    }

    };

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

        String outpostId_ssa_2;

        String hardwareType;

        String accessPointSuffix;

        String regionPrefix;

        String s3expressAvailabilityZoneId_ssa_6;

        String s3expressAvailabilityZoneId_ssa_2;

        String s3expressAvailabilityZoneId_ssa_7;

        RuleArn bucketArn;

        String uri_encoded_bucket;

        RuleUrl url;

        String s3expressAvailabilityZoneId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_8;

        String s3expressAvailabilityZoneId_ssa_3;

        String s3expressAvailabilityZoneId_ssa_4;

        String arnType;

        String s3expressAvailabilityZoneId_ssa_9;

        String s3expressAvailabilityZoneId_ssa_5;

        String s3expressAvailabilityZoneId_ssa_10;

        RulePartition bucketPartition;

        String outpostId_ssa_1;

        String accessPointName_ssa_1;

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
