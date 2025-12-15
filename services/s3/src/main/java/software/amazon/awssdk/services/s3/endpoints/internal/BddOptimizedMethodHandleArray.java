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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BddOptimizedMethodHandleArray implements S3EndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1, 0, 3, 100000200, 1, 828, 4, 2, 569, 5, 3, 163, 6, 4, 12, 7, 5, 8,
                                                  100000200, 7, 9, 10, 8, 842, 10, 29, 11, 836, 30, 127, 836, 5, 24, 13, 11, 19, 14, 12, 100000009, 15, 13, 100000009,
                                                  16, 14, 17, 21, 17, 100000172, 18, 22, 23, 100000172, 12, 100000009, 20, 13, 100000009, 21, 17, 869, 22, 22, 23,
                                                  100000200, 35, 862, 200, 7, 25, 26, 8, 36, 26, 9, 27, 28, 10, 32, 28, 11, 30, 29, 12, 31, 40, 12, 31, 42, 19, 57,
                                                  100000009, 11, 34, 33, 12, 35, 72, 12, 35, 78, 19, 149, 100000009, 9, 37, 38, 10, 70, 38, 11, 41, 39, 12, 55, 40, 13,
                                                  43, 73, 12, 55, 42, 13, 43, 79, 19, 44, 100000009, 35, 45, 241, 37, 46, 47, 38, 100000079, 47, 42, 48, 49, 43,
                                                  100000083, 49, 47, 50, 51, 53, 100000087, 51, 57, 52, 53, 58, 100000091, 53, 62, 54, 100000036, 63, 100000095,
                                                  100000036, 19, 56, 147, 28, 162, 57, 35, 58, 254, 38, 59, 60, 41, 68, 60, 45, 61, 62, 46, 100000039, 62, 48, 63, 64,
                                                  50, 100000047, 64, 51, 65, 66, 53, 100000051, 66, 60, 67, 100000036, 61, 100000055, 100000036, 45, 69, 100000043, 46,
                                                  100000039, 100000043, 11, 77, 71, 12, 146, 72, 13, 134, 73, 14, 74, 79, 17, 897, 75, 19, 107, 76, 22, 82, 897, 12,
                                                  146, 78, 13, 134, 79, 17, 118, 80, 19, 107, 81, 22, 82, 119, 23, 83, 87, 24, 84, 87, 25, 85, 87, 26, 86, 87, 27,
                                                  1012, 87, 35, 88, 323, 44, 89, 90, 49, 974, 90, 54, 967, 91, 55, 92, 100000171, 56, 93, 976, 59, 100000171, 94, 64,
                                                  984, 95, 65, 96, 950, 66, 988, 97, 70, 98, 100000143, 71, 100000143, 99, 72, 100, 102, 73, 101, 102, 74, 102,
                                                  100000132, 75, 103, 100000140, 76, 104, 100000139, 78, 105, 100000151, 79, 106, 100000138, 83, 100000148, 100000137,
                                                  23, 108, 112, 24, 109, 112, 25, 110, 112, 26, 111, 112, 27, 1012, 112, 33, 113, 100000128, 35, 117, 114, 88,
                                                  100000125, 115, 89, 116, 100000127, 90, 100000126, 100000127, 88, 100000117, 100000118, 18, 1007, 119, 23, 120, 124,
                                                  24, 121, 124, 25, 122, 124, 26, 123, 124, 27, 1012, 124, 28, 129, 125, 29, 126, 100000200, 30, 127, 100000200, 31,
                                                  128, 100000128, 35, 100000129, 100000188, 35, 133, 130, 88, 100000182, 131, 89, 132, 100000184, 90, 100000183,
                                                  100000184, 88, 100000178, 100000179, 19, 135, 100000009, 35, 136, 536, 37, 137, 138, 38, 100000059, 138, 42, 139,
                                                  140, 43, 100000063, 140, 47, 141, 142, 53, 100000067, 142, 57, 143, 144, 58, 100000071, 144, 62, 145, 100000036, 63,
                                                  100000075, 100000036, 19, 148, 147, 28, 162, 100000009, 28, 162, 149, 35, 150, 549, 38, 151, 152, 41, 160, 152, 45,
                                                  153, 154, 46, 100000018, 154, 48, 155, 156, 50, 100000026, 156, 51, 157, 158, 53, 100000030, 158, 60, 159, 100000036,
                                                  61, 100000034, 100000036, 45, 161, 100000022, 46, 100000018, 100000022, 35, 100000014, 100000015, 4, 178, 164, 5,
                                                  165, 515, 7, 166, 167, 8, 175, 167, 16, 170, 168, 29, 169, 517, 30, 390, 517, 29, 171, 172, 30, 516, 172, 31, 173,
                                                  517, 35, 100000002, 174, 88, 100000195, 100000196, 16, 177, 176, 35, 100000002, 100000101, 35, 100000002, 100000097,
                                                  5, 231, 179, 9, 180, 181, 10, 187, 181, 11, 184, 182, 12, 186, 183, 13, 186, 190, 12, 186, 185, 13, 186, 196, 16,
                                                  266, 564, 11, 194, 188, 12, 230, 189, 13, 230, 190, 14, 191, 196, 15, 192, 312, 17, 297, 193, 22, 199, 297, 12, 230,
                                                  195, 13, 230, 196, 15, 197, 312, 17, 229, 198, 22, 199, 515, 35, 100000002, 200, 44, 201, 202, 49, 217, 202, 52, 208,
                                                  203, 54, 206, 204, 55, 205, 100000171, 56, 211, 220, 55, 207, 100000171, 56, 215, 220, 54, 213, 209, 55, 210,
                                                  100000171, 56, 211, 475, 59, 100000171, 212, 64, 225, 951, 55, 214, 100000171, 56, 215, 475, 59, 100000171, 216, 64,
                                                  225, 972, 52, 221, 218, 55, 219, 100000171, 56, 223, 220, 59, 100000171, 453, 55, 222, 100000171, 56, 223, 475, 59,
                                                  100000171, 224, 64, 225, 982, 66, 732, 226, 70, 227, 100000143, 71, 100000143, 228, 84, 486, 100000158, 18, 526, 515,
                                                  16, 561, 564, 9, 232, 233, 10, 273, 233, 11, 236, 234, 12, 251, 235, 13, 238, 276, 12, 251, 237, 13, 238, 300, 16,
                                                  266, 239, 19, 240, 564, 35, 100000002, 241, 37, 242, 243, 38, 100000080, 243, 42, 244, 245, 43, 100000084, 245, 47,
                                                  246, 247, 53, 100000088, 247, 57, 248, 249, 58, 100000092, 249, 62, 250, 100000036, 63, 100000096, 100000036, 16,
                                                  266, 252, 19, 253, 564, 35, 100000002, 254, 38, 255, 256, 41, 264, 256, 45, 257, 258, 46, 100000040, 258, 48, 259,
                                                  260, 50, 100000048, 260, 51, 261, 262, 53, 100000052, 262, 60, 263, 100000036, 61, 100000056, 100000036, 45, 265,
                                                  100000044, 46, 100000040, 100000044, 19, 269, 267, 28, 268, 564, 34, 272, 564, 28, 270, 271, 34, 272, 271, 35,
                                                  100000002, 100000011, 35, 100000002, 100000010, 11, 298, 274, 12, 546, 275, 13, 533, 276, 14, 277, 300, 15, 278, 301,
                                                  16, 287, 279, 17, 282, 280, 19, 376, 281, 22, 317, 282, 23, 283, 297, 24, 284, 297, 25, 285, 297, 26, 286, 297, 27,
                                                  528, 297, 17, 292, 288, 19, 494, 289, 20, 290, 291, 21, 487, 291, 22, 403, 292, 23, 293, 297, 24, 294, 297, 25, 295,
                                                  297, 26, 296, 297, 27, 527, 297, 35, 100000002, 100000172, 12, 546, 299, 13, 533, 300, 15, 313, 301, 16, 307, 302,
                                                  23, 303, 312, 24, 304, 312, 25, 305, 312, 26, 306, 312, 27, 528, 312, 23, 308, 312, 24, 309, 312, 25, 310, 312, 26,
                                                  311, 312, 27, 527, 312, 35, 100000002, 100000109, 16, 398, 314, 17, 381, 315, 19, 376, 316, 22, 317, 382, 23, 318,
                                                  322, 24, 319, 322, 25, 320, 322, 26, 321, 322, 27, 528, 322, 35, 100000002, 323, 44, 324, 325, 49, 358, 325, 52, 331,
                                                  326, 54, 329, 327, 55, 328, 100000171, 56, 334, 361, 55, 330, 100000171, 56, 446, 361, 54, 444, 332, 55, 333,
                                                  100000171, 56, 334, 475, 59, 100000171, 335, 64, 480, 336, 65, 337, 951, 66, 348, 338, 70, 339, 100000143, 71,
                                                  100000143, 340, 72, 341, 343, 73, 342, 343, 74, 343, 100000132, 75, 344, 100000140, 76, 345, 100000139, 78, 346,
                                                  100000151, 79, 347, 100000138, 83, 100000150, 100000137, 70, 349, 100000143, 71, 100000143, 350, 72, 351, 353, 73,
                                                  352, 353, 74, 353, 100000132, 75, 354, 100000140, 76, 355, 100000139, 77, 100000133, 356, 79, 357, 100000138, 83,
                                                  100000136, 100000137, 52, 473, 359, 55, 360, 100000171, 56, 478, 361, 59, 100000171, 362, 65, 363, 453, 66,
                                                  100000144, 364, 67, 365, 100000170, 68, 366, 100000169, 69, 367, 100000168, 72, 368, 370, 73, 369, 370, 74, 370,
                                                  100000132, 75, 371, 100000140, 76, 372, 100000139, 79, 373, 100000138, 80, 374, 100000167, 81, 375, 100000166, 82,
                                                  100000164, 100000165, 23, 377, 517, 24, 378, 517, 25, 379, 517, 26, 380, 517, 27, 528, 517, 18, 393, 382, 23, 383,
                                                  387, 24, 384, 387, 25, 385, 387, 26, 386, 387, 27, 528, 387, 28, 392, 388, 29, 389, 515, 30, 390, 515, 31, 391, 517,
                                                  35, 100000002, 100000188, 35, 100000002, 100000185, 23, 394, 526, 24, 395, 526, 25, 396, 526, 26, 397, 526, 27, 528,
                                                  526, 17, 506, 399, 19, 494, 400, 20, 401, 402, 21, 487, 402, 22, 403, 507, 23, 404, 408, 24, 405, 408, 25, 406, 408,
                                                  26, 407, 408, 27, 527, 408, 35, 100000002, 409, 44, 410, 411, 49, 448, 411, 52, 417, 412, 54, 415, 413, 55, 414,
                                                  100000171, 56, 420, 451, 55, 416, 100000171, 56, 446, 451, 54, 444, 418, 55, 419, 100000171, 56, 420, 475, 59,
                                                  100000171, 421, 64, 480, 422, 65, 423, 951, 66, 434, 424, 70, 425, 100000143, 71, 100000143, 426, 72, 427, 429, 73,
                                                  428, 429, 74, 429, 100000132, 75, 430, 100000140, 76, 431, 100000139, 78, 432, 100000151, 79, 433, 100000138, 83,
                                                  100000149, 100000137, 70, 435, 100000143, 71, 100000143, 436, 72, 437, 439, 73, 438, 439, 74, 439, 100000132, 75,
                                                  440, 100000140, 76, 441, 100000139, 77, 100000133, 442, 79, 443, 100000138, 83, 100000134, 100000137, 55, 445,
                                                  100000171, 56, 446, 475, 59, 100000171, 447, 64, 480, 972, 52, 473, 449, 55, 450, 100000171, 56, 478, 451, 59,
                                                  100000171, 452, 65, 460, 453, 66, 100000144, 454, 67, 455, 100000170, 68, 456, 100000169, 69, 457, 100000168, 72,
                                                  458, 100000168, 73, 459, 100000168, 74, 100000168, 100000132, 66, 100000144, 461, 67, 462, 100000170, 68, 463,
                                                  100000169, 69, 464, 100000168, 72, 465, 467, 73, 466, 467, 74, 467, 100000132, 75, 468, 100000140, 76, 469,
                                                  100000139, 79, 470, 100000138, 80, 471, 100000167, 81, 472, 100000166, 82, 100000163, 100000165, 55, 474, 100000171,
                                                  56, 478, 475, 59, 100000171, 476, 66, 100000144, 477, 67, 100000162, 100000170, 59, 100000171, 479, 64, 480, 982, 66,
                                                  732, 481, 70, 482, 100000143, 71, 100000143, 483, 84, 486, 484, 85, 485, 100000158, 86, 100000156, 100000157, 85,
                                                  100000155, 100000158, 23, 488, 492, 24, 489, 492, 25, 490, 492, 26, 491, 492, 27, 527, 492, 33, 493, 517, 35,
                                                  100000002, 100000122, 23, 495, 499, 24, 496, 499, 25, 497, 499, 26, 498, 499, 27, 527, 499, 33, 500, 517, 34, 504,
                                                  501, 35, 100000002, 502, 87, 503, 100000128, 88, 100000120, 100000122, 35, 100000002, 505, 88, 100000119, 100000121,
                                                  18, 521, 507, 23, 508, 512, 24, 509, 512, 25, 510, 512, 26, 511, 512, 27, 527, 512, 28, 519, 513, 29, 514, 515, 30,
                                                  516, 515, 35, 100000002, 100000200, 31, 518, 517, 35, 100000002, 100000128, 35, 100000002, 100000186, 35, 100000002,
                                                  520, 88, 100000180, 100000181, 23, 522, 526, 24, 523, 526, 25, 524, 526, 26, 525, 526, 27, 527, 526, 35, 100000002,
                                                  100000173, 32, 529, 528, 35, 100000002, 1013, 35, 100000002, 530, 36, 531, 100000108, 39, 100000103, 532, 40,
                                                  100000105, 100000107, 16, 561, 534, 19, 535, 564, 35, 100000002, 536, 37, 537, 538, 38, 100000060, 538, 42, 539, 540,
                                                  43, 100000064, 540, 47, 541, 542, 53, 100000068, 542, 57, 543, 544, 58, 100000072, 544, 62, 545, 100000036, 63,
                                                  100000076, 100000036, 16, 561, 547, 19, 548, 564, 35, 100000002, 549, 38, 550, 551, 41, 559, 551, 45, 552, 553, 46,
                                                  100000019, 553, 48, 554, 555, 50, 100000027, 555, 51, 556, 557, 53, 100000031, 557, 60, 558, 100000036, 61,
                                                  100000035, 100000036, 45, 560, 100000023, 46, 100000019, 100000023, 19, 565, 562, 28, 563, 564, 34, 568, 564, 35,
                                                  100000002, 100000009, 28, 566, 567, 34, 568, 567, 35, 100000002, 100000008, 35, 100000002, 100000007, 3, 827, 570, 4,
                                                  582, 571, 5, 572, 100000200, 6, 100000005, 573, 7, 574, 575, 8, 581, 575, 29, 576, 577, 30, 771, 577, 31, 578,
                                                  100000128, 35, 580, 579, 88, 100000191, 100000192, 88, 100000189, 100000190, 35, 100000098, 100000099, 5, 601, 583,
                                                  11, 589, 584, 12, 100000009, 585, 13, 100000009, 586, 14, 587, 591, 17, 100000172, 588, 22, 593, 100000172, 12,
                                                  100000009, 590, 13, 100000009, 591, 17, 869, 592, 22, 593, 100000200, 35, 862, 594, 44, 595, 596, 49, 721, 596, 54,
                                                  717, 597, 55, 598, 100000171, 56, 599, 723, 59, 100000171, 600, 64, 728, 951, 6, 100000005, 602, 7, 603, 604, 8, 614,
                                                  604, 9, 605, 606, 10, 610, 606, 11, 608, 607, 12, 609, 618, 12, 609, 620, 19, 645, 100000009, 11, 612, 611, 12, 613,
                                                  672, 12, 613, 678, 19, 801, 100000009, 9, 615, 616, 10, 670, 616, 11, 619, 617, 12, 643, 618, 13, 621, 673, 12, 643,
                                                  620, 13, 621, 679, 19, 622, 100000009, 35, 633, 623, 37, 624, 625, 38, 100000078, 625, 42, 626, 627, 43, 100000082,
                                                  627, 47, 628, 629, 53, 100000086, 629, 57, 630, 631, 58, 100000090, 631, 62, 632, 100000036, 63, 100000094,
                                                  100000036, 37, 634, 635, 38, 100000077, 635, 42, 636, 637, 43, 100000081, 637, 47, 638, 639, 53, 100000085, 639, 57,
                                                  640, 641, 58, 100000089, 641, 62, 642, 100000036, 63, 100000093, 100000036, 19, 644, 799, 28, 826, 645, 35, 658, 646,
                                                  38, 647, 648, 41, 656, 648, 45, 649, 650, 46, 100000038, 650, 48, 651, 652, 50, 100000046, 652, 51, 653, 654, 53,
                                                  100000050, 654, 60, 655, 100000036, 61, 100000054, 100000036, 45, 657, 100000042, 46, 100000038, 100000042, 38, 659,
                                                  660, 41, 668, 660, 45, 661, 662, 46, 100000037, 662, 48, 663, 664, 50, 100000045, 664, 51, 665, 666, 53, 100000049,
                                                  666, 60, 667, 100000036, 61, 100000053, 100000036, 45, 669, 100000041, 46, 100000037, 100000041, 11, 677, 671, 12,
                                                  798, 672, 13, 776, 673, 14, 674, 679, 17, 897, 675, 19, 753, 676, 22, 682, 897, 12, 798, 678, 13, 776, 679, 17, 762,
                                                  680, 19, 753, 681, 22, 682, 763, 23, 683, 687, 24, 684, 687, 25, 685, 687, 26, 686, 687, 27, 1012, 687, 35, 734, 688,
                                                  44, 689, 690, 49, 721, 690, 54, 717, 691, 55, 692, 100000171, 56, 693, 723, 59, 100000171, 694, 64, 728, 695, 65,
                                                  696, 951, 66, 707, 697, 70, 698, 100000143, 71, 100000143, 699, 72, 700, 702, 73, 701, 702, 74, 702, 100000132, 75,
                                                  703, 100000140, 76, 704, 100000139, 78, 705, 100000151, 79, 706, 100000138, 83, 100000147, 100000137, 70, 708,
                                                  100000143, 71, 100000143, 709, 72, 710, 712, 73, 711, 712, 74, 712, 100000132, 75, 713, 100000140, 76, 714,
                                                  100000139, 77, 100000133, 715, 79, 716, 100000138, 83, 100000135, 100000137, 55, 718, 100000171, 56, 719, 723, 59,
                                                  100000171, 720, 64, 728, 972, 55, 722, 100000171, 56, 726, 723, 59, 100000171, 724, 66, 100000144, 725, 67,
                                                  100000160, 100000170, 59, 100000171, 727, 64, 728, 982, 66, 732, 729, 70, 730, 100000143, 71, 100000143, 731, 85,
                                                  100000153, 100000158, 70, 733, 100000143, 71, 100000143, 100000142, 44, 735, 736, 49, 974, 736, 54, 967, 737, 55,
                                                  738, 100000171, 56, 739, 976, 59, 100000171, 740, 64, 984, 741, 65, 742, 950, 66, 988, 743, 70, 744, 100000143, 71,
                                                  100000143, 745, 72, 746, 748, 73, 747, 748, 74, 748, 100000132, 75, 749, 100000140, 76, 750, 100000139, 78, 751,
                                                  100000151, 79, 752, 100000138, 83, 100000146, 100000137, 23, 754, 758, 24, 755, 758, 25, 756, 758, 26, 757, 758, 27,
                                                  1012, 758, 33, 759, 100000128, 35, 761, 760, 88, 100000113, 100000114, 88, 100000111, 100000112, 18, 1007, 763, 23,
                                                  764, 768, 24, 765, 768, 25, 766, 768, 26, 767, 768, 27, 1012, 768, 28, 773, 769, 29, 770, 100000200, 30, 771,
                                                  100000200, 31, 772, 100000128, 35, 100000129, 100000187, 35, 775, 774, 88, 100000176, 100000177, 88, 100000174,
                                                  100000175, 19, 777, 100000009, 35, 788, 778, 37, 779, 780, 38, 100000058, 780, 42, 781, 782, 43, 100000062, 782, 47,
                                                  783, 784, 53, 100000066, 784, 57, 785, 786, 58, 100000070, 786, 62, 787, 100000036, 63, 100000074, 100000036, 37,
                                                  789, 790, 38, 100000057, 790, 42, 791, 792, 43, 100000061, 792, 47, 793, 794, 53, 100000065, 794, 57, 795, 796, 58,
                                                  100000069, 796, 62, 797, 100000036, 63, 100000073, 100000036, 19, 800, 799, 28, 826, 100000009, 28, 826, 801, 35,
                                                  814, 802, 38, 803, 804, 41, 812, 804, 45, 805, 806, 46, 100000017, 806, 48, 807, 808, 50, 100000025, 808, 51, 809,
                                                  810, 53, 100000029, 810, 60, 811, 100000036, 61, 100000033, 100000036, 45, 813, 100000021, 46, 100000017, 100000021,
                                                  38, 815, 816, 41, 824, 816, 45, 817, 818, 46, 100000016, 818, 48, 819, 820, 50, 100000024, 820, 51, 821, 822, 53,
                                                  100000028, 822, 60, 823, 100000036, 61, 100000032, 100000036, 45, 825, 100000020, 46, 100000016, 100000020, 35,
                                                  100000012, 100000013, 35, 100000002, 100000003, 2, 100000001, 829, 3, 1019, 830, 4, 843, 831, 5, 832, 100000200, 7,
                                                  833, 834, 8, 842, 834, 29, 835, 836, 30, 1005, 836, 31, 837, 100000128, 35, 841, 838, 88, 100000197, 839, 89, 840,
                                                  100000199, 90, 100000198, 100000199, 88, 100000193, 100000194, 35, 100000100, 100000101, 5, 870, 844, 11, 850, 845,
                                                  12, 100000006, 846, 13, 100000006, 847, 14, 848, 852, 17, 100000172, 849, 22, 854, 100000172, 12, 100000006, 851, 13,
                                                  100000006, 852, 17, 869, 853, 22, 854, 100000200, 35, 862, 855, 44, 856, 857, 49, 928, 857, 54, 923, 858, 55, 859,
                                                  100000171, 56, 860, 930, 59, 100000171, 861, 64, 936, 921, 44, 863, 864, 49, 974, 864, 54, 967, 865, 55, 866,
                                                  100000171, 56, 867, 976, 59, 100000171, 868, 64, 984, 950, 18, 100000173, 100000200, 6, 890, 871, 11, 877, 872, 12,
                                                  100000006, 873, 13, 100000006, 874, 14, 875, 879, 17, 897, 876, 19, 881, 896, 12, 100000006, 878, 13, 100000006, 879,
                                                  17, 996, 880, 19, 881, 906, 23, 882, 886, 24, 883, 886, 25, 884, 886, 26, 885, 886, 27, 1012, 886, 33, 887,
                                                  100000128, 35, 889, 888, 88, 100000123, 100000124, 88, 100000115, 100000116, 11, 902, 891, 12, 100000006, 892, 13,
                                                  100000006, 893, 14, 894, 904, 17, 897, 895, 19, 990, 896, 22, 907, 897, 23, 898, 100000172, 24, 899, 100000172, 25,
                                                  900, 100000172, 26, 901, 100000172, 27, 1012, 100000172, 12, 100000006, 903, 13, 100000006, 904, 17, 996, 905, 19,
                                                  990, 906, 22, 907, 997, 23, 908, 912, 24, 909, 912, 25, 910, 912, 26, 911, 912, 27, 1012, 912, 35, 942, 913, 44, 914,
                                                  915, 49, 928, 915, 54, 923, 916, 55, 917, 100000171, 56, 918, 930, 59, 100000171, 919, 64, 936, 920, 65, 922, 921,
                                                  66, 940, 951, 66, 940, 957, 55, 924, 100000171, 56, 925, 930, 59, 100000171, 926, 64, 936, 927, 66, 940, 972, 55,
                                                  929, 100000171, 56, 933, 930, 59, 100000171, 931, 66, 100000144, 932, 67, 100000161, 100000170, 59, 100000171, 934,
                                                  64, 936, 935, 66, 940, 982, 66, 940, 937, 70, 938, 100000143, 71, 100000143, 939, 85, 100000154, 100000158, 70, 941,
                                                  100000143, 71, 100000143, 100000130, 44, 943, 944, 49, 974, 944, 54, 967, 945, 55, 946, 100000171, 56, 947, 976, 59,
                                                  100000171, 948, 64, 984, 949, 65, 956, 950, 66, 988, 951, 70, 952, 100000143, 71, 100000143, 953, 72, 954, 100000141,
                                                  73, 955, 100000141, 74, 100000141, 100000132, 66, 988, 957, 70, 958, 100000143, 71, 100000143, 959, 72, 960, 962, 73,
                                                  961, 962, 74, 962, 100000132, 75, 963, 100000140, 76, 964, 100000139, 78, 965, 100000151, 79, 966, 100000138, 83,
                                                  100000145, 100000137, 55, 968, 100000171, 56, 969, 976, 59, 100000171, 970, 64, 984, 971, 66, 988, 972, 70, 973,
                                                  100000143, 71, 100000143, 100000141, 55, 975, 100000171, 56, 979, 976, 59, 100000171, 977, 66, 100000144, 978, 67,
                                                  100000159, 100000170, 59, 100000171, 980, 64, 984, 981, 66, 988, 982, 70, 983, 100000143, 71, 100000143, 100000131,
                                                  66, 988, 985, 70, 986, 100000143, 71, 100000143, 987, 85, 100000152, 100000158, 70, 989, 100000143, 71, 100000143,
                                                  100000129, 23, 991, 995, 24, 992, 995, 25, 993, 995, 26, 994, 995, 27, 1012, 995, 33, 100000110, 100000128, 18, 1007,
                                                  997, 23, 998, 1002, 24, 999, 1002, 25, 1000, 1002, 26, 1001, 1002, 27, 1012, 1002, 28, 100000185, 1003, 29, 1004,
                                                  100000200, 30, 1005, 100000200, 31, 1006, 100000128, 35, 100000129, 100000130, 23, 1008, 100000173, 24, 1009,
                                                  100000173, 25, 1010, 100000173, 26, 1011, 100000173, 27, 1012, 100000173, 32, 1016, 1013, 36, 1014, 100000108, 39,
                                                  100000104, 1015, 40, 100000106, 100000107, 36, 1017, 100000108, 39, 100000102, 1018, 40, 100000102, 100000107, 35,
                                                  100000002, 100000004 };

    private static MethodHandle[] RESULT_FNS;

    private static MethodHandle[] CONDITION_FNS;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CONDITION_FNS = new MethodHandle[] {
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c0",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c1",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c2",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c3",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c4",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c5",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c6",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c7",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c8",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c9",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c10",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c11",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c12",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c13",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c14",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c15",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c16",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c17",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c18",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c19",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c20",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c21",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c22",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c23",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c24",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c25",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c26",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c27",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c28",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c29",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c30",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c31",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c32",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c33",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c34",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c35",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c36",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c37",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c38",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c39",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c40",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c41",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c42",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c43",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c44",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c45",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c46",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c47",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c48",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c49",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c50",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c51",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c52",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c53",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c54",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c55",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c56",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c57",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c58",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c59",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c60",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c61",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c62",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c63",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c64",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c65",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c66",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c67",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c68",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c69",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c70",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c71",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c72",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c73",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c74",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c75",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c76",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c77",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c78",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c79",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c80",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c81",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c82",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c83",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c84",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c85",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c86",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c87",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c88",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c89",
                                  MethodType.methodType(boolean.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "c90",
                                  MethodType.methodType(boolean.class, Registers.class)) };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            RESULT_FNS = new MethodHandle[] {
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r0",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r1",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r2",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r3",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r4",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r5",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r6",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r7",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r8",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r9",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r10",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r11",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r12",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r13",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r14",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r15",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r16",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r17",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r18",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r19",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r20",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r21",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r22",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r23",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r24",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r25",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r26",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r27",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r28",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r29",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r30",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r31",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r32",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r33",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r34",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r35",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r36",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r37",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r38",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r39",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r40",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r41",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r42",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r43",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r44",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r45",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r46",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r47",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r48",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r49",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r50",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r51",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r52",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r53",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r54",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r55",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r56",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r57",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r58",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r59",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r60",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r61",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r62",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r63",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r64",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r65",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r66",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r67",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r68",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r69",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r70",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r71",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r72",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r73",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r74",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r75",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r76",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r77",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r78",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r79",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r80",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r81",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r82",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r83",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r84",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r85",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r86",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r87",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r88",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r89",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r90",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r91",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r92",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r93",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r94",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r95",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r96",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r97",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r98",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r99",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r100",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r101",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r102",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r103",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r104",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r105",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r106",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r107",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r108",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r109",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r110",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r111",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r112",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r113",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r114",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r115",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r116",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r117",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r118",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r119",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r120",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r121",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r122",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r123",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r124",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r125",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r126",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r127",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r128",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r129",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r130",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r131",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r132",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r133",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r134",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r135",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r136",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r137",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r138",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r139",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r140",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r141",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r142",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r143",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r144",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r145",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r146",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r147",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r148",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r149",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r150",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r151",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r152",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r153",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r154",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r155",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r156",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r157",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r158",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r159",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r160",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r161",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r162",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r163",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r164",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r165",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r166",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r167",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r168",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r169",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r170",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r171",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r172",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r173",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r174",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r175",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r176",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r177",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r178",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r179",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r180",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r181",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r182",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r183",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r184",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r185",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r186",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r187",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r188",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r189",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r190",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r191",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r192",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r193",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r194",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r195",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r196",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r197",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r198",
                                  MethodType.methodType(RuleResult.class, Registers.class)),
                lookup.findStatic(BddOptimizedMethodHandleArray.class, "r199",
                                  MethodType.methodType(RuleResult.class, Registers.class)) };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean c0(Registers registers) {
        return (registers.region != null);
    }

    private static boolean c1(Registers registers) {
        return (registers.accelerate);
    }

    private static boolean c2(Registers registers) {
        return (registers.useFIPS);
    }

    private static boolean c3(Registers registers) {
        return (registers.endpoint != null);
    }

    private static boolean c4(Registers registers) {
        return (registers.bucket != null);
    }

    private static boolean c5(Registers registers) {
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        return registers.partitionResult != null;
    }

    private static boolean c6(Registers registers) {
        return ("aws-cn".equals(registers.partitionResult.name()));
    }

    private static boolean c7(Registers registers) {
        return (registers.useS3ExpressControlEndpoint != null);
    }

    private static boolean c8(Registers registers) {
        return (registers.useS3ExpressControlEndpoint);
    }

    private static boolean c9(Registers registers) {
        return (registers.disableS3ExpressSessionAuth != null);
    }

    private static boolean c10(Registers registers) {
        return (registers.disableS3ExpressSessionAuth);
    }

    private static boolean c11(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean c12(Registers registers) {
        return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 6, true), "")));
    }

    private static boolean c13(Registers registers) {
        return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 7, true), "")));
    }

    private static boolean c14(Registers registers) {
        return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), "")));
    }

    private static boolean c15(Registers registers) {
        return (RulesFunctions.parseURL(registers.endpoint) != null);
    }

    private static boolean c16(Registers registers) {
        registers.url = RulesFunctions.parseURL(registers.endpoint);
        return registers.url != null;
    }

    private static boolean c17(Registers registers) {
        return (registers.forcePathStyle);
    }

    private static boolean c18(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean c19(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, false));
    }

    private static boolean c20(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, true));
    }

    private static boolean c21(Registers registers) {
        return ("http".equals(registers.url.scheme()));
    }

    private static boolean c22(Registers registers) {
        registers.bucketArn = RulesFunctions.awsParseArn(registers.bucket);
        return registers.bucketArn != null;
    }

    private static boolean c23(Registers registers) {
        registers.outpostId_ssa_2 = RulesFunctions.substring(registers.bucket, 32, 49, true);
        return registers.outpostId_ssa_2 != null;
    }

    private static boolean c24(Registers registers) {
        registers.hardwareType = RulesFunctions.substring(registers.bucket, 49, 50, true);
        return registers.hardwareType != null;
    }

    private static boolean c25(Registers registers) {
        registers.accessPointSuffix = RulesFunctions.substring(registers.bucket, 0, 7, true);
        return registers.accessPointSuffix != null;
    }

    private static boolean c26(Registers registers) {
        return ("--op-s3".equals(registers.accessPointSuffix));
    }

    private static boolean c27(Registers registers) {
        registers.regionPrefix = RulesFunctions.substring(registers.bucket, 8, 12, true);
        return registers.regionPrefix != null;
    }

    private static boolean c28(Registers registers) {
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        return registers.uri_encoded_bucket != null;
    }

    private static boolean c29(Registers registers) {
        return (registers.useObjectLambdaEndpoint != null);
    }

    private static boolean c30(Registers registers) {
        return (registers.useObjectLambdaEndpoint);
    }

    private static boolean c31(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, true));
    }

    private static boolean c32(Registers registers) {
        return ("beta".equals(registers.regionPrefix));
    }

    private static boolean c33(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, false));
    }

    private static boolean c34(Registers registers) {
        return (registers.url.isIp());
    }

    private static boolean c35(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean c36(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
    }

    private static boolean c37(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_6 != null;
    }

    private static boolean c38(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), "")));
    }

    private static boolean c39(Registers registers) {
        return ("e".equals(registers.hardwareType));
    }

    private static boolean c40(Registers registers) {
        return ("o".equals(registers.hardwareType));
    }

    private static boolean c41(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_2 != null;
    }

    private static boolean c42(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), "")));
    }

    private static boolean c43(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        return registers.s3expressAvailabilityZoneId_ssa_7 != null;
    }

    private static boolean c44(Registers registers) {
        return (registers.disableAccessPoints != null);
    }

    private static boolean c45(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), "")));
    }

    private static boolean c46(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        return registers.s3expressAvailabilityZoneId_ssa_1 != null;
    }

    private static boolean c47(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_8 != null;
    }

    private static boolean c48(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), "")));
    }

    private static boolean c49(Registers registers) {
        return (registers.disableAccessPoints);
    }

    private static boolean c50(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        return registers.s3expressAvailabilityZoneId_ssa_3 != null;
    }

    private static boolean c51(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_4 != null;
    }

    private static boolean c52(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
    }

    private static boolean c53(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), "")));
    }

    private static boolean c54(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
    }

    private static boolean c55(Registers registers) {
        registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
        return registers.arnType != null;
    }

    private static boolean c56(Registers registers) {
        return ("accesspoint".equals(registers.arnType));
    }

    private static boolean c57(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), "")));
    }

    private static boolean c58(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        return registers.s3expressAvailabilityZoneId_ssa_9 != null;
    }

    private static boolean c59(Registers registers) {
        return ("".equals(registers.arnType));
    }

    private static boolean c60(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        return registers.s3expressAvailabilityZoneId_ssa_5 != null;
    }

    private static boolean c61(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), "")));
    }

    private static boolean c62(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        return registers.s3expressAvailabilityZoneId_ssa_10 != null;
    }

    private static boolean c63(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), "")));
    }

    private static boolean c64(Registers registers) {
        return ("".equals(registers.bucketArn.region()));
    }

    private static boolean c65(Registers registers) {
        registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
        return registers.bucketPartition != null;
    }

    private static boolean c66(Registers registers) {
        return ("s3-object-lambda".equals(registers.bucketArn.service()));
    }

    private static boolean c67(Registers registers) {
        return ("s3-outposts".equals(registers.bucketArn.service()));
    }

    private static boolean c68(Registers registers) {
        registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.outpostId_ssa_1 != null;
    }

    private static boolean c69(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
    }

    private static boolean c70(Registers registers) {
        registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.accessPointName_ssa_1 != null;
    }

    private static boolean c71(Registers registers) {
        return ("".equals(registers.accessPointName_ssa_1));
    }

    private static boolean c72(Registers registers) {
        return (registers.useArnRegion != null);
    }

    private static boolean c73(Registers registers) {
        return (!registers.useArnRegion);
    }

    private static boolean c74(Registers registers) {
        return (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region()));
    }

    private static boolean c75(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
    }

    private static boolean c76(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
    }

    private static boolean c77(Registers registers) {
        return ("".equals(registers.bucketArn.accountId()));
    }

    private static boolean c78(Registers registers) {
        return ("s3".equals(registers.bucketArn.service()));
    }

    private static boolean c79(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
    }

    private static boolean c80(Registers registers) {
        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
        return registers.outpostType != null;
    }

    private static boolean c81(Registers registers) {
        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
        return registers.accessPointName_ssa_2 != null;
    }

    private static boolean c82(Registers registers) {
        return ("accesspoint".equals(registers.outpostType));
    }

    private static boolean c83(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
    }

    private static boolean c84(Registers registers) {
        return (registers.disableMultiRegionAccessPoints);
    }

    private static boolean c85(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
    }

    private static boolean c86(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
    }

    private static boolean c87(Registers registers) {
        return (!registers.url.isIp());
    }

    private static boolean c88(Registers registers) {
        return ("aws-global".equals(registers.region));
    }

    private static boolean c89(Registers registers) {
        return ("us-east-1".equals(registers.region));
    }

    private static boolean c90(Registers registers) {
        return (registers.useGlobalEndpoint);
    }

    private static RuleResult r0(Registers registers) {
        return RuleResult.error("Accelerate cannot be used with FIPS");
    }

    private static RuleResult r1(Registers registers) {
        return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
    }

    private static RuleResult r2(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with FIPS");
    }

    private static RuleResult r3(Registers registers) {
        return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
    }

    private static RuleResult r4(Registers registers) {
        return RuleResult.error("Partition does not support FIPS");
    }

    private static RuleResult r5(Registers registers) {
        return RuleResult.error("S3Express does not support S3 Accelerate.");
    }

    private static RuleResult r6(Registers registers) {
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

    private static RuleResult r7(Registers registers) {
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

    private static RuleResult r8(Registers registers) {
        return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
    }

    private static RuleResult r9(Registers registers) {
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

    private static RuleResult r10(Registers registers) {
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

    private static RuleResult r11(Registers registers) {
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

    private static RuleResult r12(Registers registers) {
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

    private static RuleResult r13(Registers registers) {
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

    private static RuleResult r14(Registers registers) {
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

    private static RuleResult r15(Registers registers) {
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

    private static RuleResult r16(Registers registers) {
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

    private static RuleResult r17(Registers registers) {
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

    private static RuleResult r18(Registers registers) {
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

    private static RuleResult r19(Registers registers) {
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

    private static RuleResult r20(Registers registers) {
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

    private static RuleResult r21(Registers registers) {
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

    private static RuleResult r22(Registers registers) {
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

    private static RuleResult r23(Registers registers) {
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

    private static RuleResult r24(Registers registers) {
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

    private static RuleResult r25(Registers registers) {
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

    private static RuleResult r26(Registers registers) {
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

    private static RuleResult r27(Registers registers) {
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

    private static RuleResult r28(Registers registers) {
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

    private static RuleResult r29(Registers registers) {
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

    private static RuleResult r30(Registers registers) {
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

    private static RuleResult r31(Registers registers) {
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

    private static RuleResult r32(Registers registers) {
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

    private static RuleResult r33(Registers registers) {
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

    private static RuleResult r34(Registers registers) {
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

    private static RuleResult r35(Registers registers) {
        return RuleResult.error("Unrecognized S3Express bucket name format.");
    }

    private static RuleResult r36(Registers registers) {
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

    private static RuleResult r37(Registers registers) {
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

    private static RuleResult r38(Registers registers) {
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

    private static RuleResult r39(Registers registers) {
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

    private static RuleResult r40(Registers registers) {
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

    private static RuleResult r41(Registers registers) {
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

    private static RuleResult r42(Registers registers) {
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

    private static RuleResult r43(Registers registers) {
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

    private static RuleResult r44(Registers registers) {
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

    private static RuleResult r45(Registers registers) {
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

    private static RuleResult r46(Registers registers) {
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

    private static RuleResult r47(Registers registers) {
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

    private static RuleResult r48(Registers registers) {
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

    private static RuleResult r49(Registers registers) {
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

    private static RuleResult r50(Registers registers) {
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

    private static RuleResult r51(Registers registers) {
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

    private static RuleResult r52(Registers registers) {
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

    private static RuleResult r53(Registers registers) {
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

    private static RuleResult r54(Registers registers) {
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

    private static RuleResult r55(Registers registers) {
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

    private static RuleResult r56(Registers registers) {
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

    private static RuleResult r57(Registers registers) {
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

    private static RuleResult r58(Registers registers) {
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

    private static RuleResult r59(Registers registers) {
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

    private static RuleResult r60(Registers registers) {
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

    private static RuleResult r61(Registers registers) {
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

    private static RuleResult r62(Registers registers) {
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

    private static RuleResult r63(Registers registers) {
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

    private static RuleResult r64(Registers registers) {
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

    private static RuleResult r65(Registers registers) {
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

    private static RuleResult r66(Registers registers) {
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

    private static RuleResult r67(Registers registers) {
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

    private static RuleResult r68(Registers registers) {
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

    private static RuleResult r69(Registers registers) {
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

    private static RuleResult r70(Registers registers) {
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

    private static RuleResult r71(Registers registers) {
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

    private static RuleResult r72(Registers registers) {
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

    private static RuleResult r73(Registers registers) {
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

    private static RuleResult r74(Registers registers) {
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

    private static RuleResult r75(Registers registers) {
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

    private static RuleResult r76(Registers registers) {
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

    private static RuleResult r77(Registers registers) {
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

    private static RuleResult r78(Registers registers) {
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

    private static RuleResult r79(Registers registers) {
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

    private static RuleResult r80(Registers registers) {
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

    private static RuleResult r81(Registers registers) {
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

    private static RuleResult r82(Registers registers) {
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

    private static RuleResult r83(Registers registers) {
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

    private static RuleResult r84(Registers registers) {
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

    private static RuleResult r85(Registers registers) {
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

    private static RuleResult r86(Registers registers) {
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

    private static RuleResult r87(Registers registers) {
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

    private static RuleResult r88(Registers registers) {
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

    private static RuleResult r89(Registers registers) {
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

    private static RuleResult r90(Registers registers) {
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

    private static RuleResult r91(Registers registers) {
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

    private static RuleResult r92(Registers registers) {
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

    private static RuleResult r93(Registers registers) {
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

    private static RuleResult r94(Registers registers) {
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

    private static RuleResult r95(Registers registers) {
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

    private static RuleResult r96(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r97(Registers registers) {
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

    private static RuleResult r98(Registers registers) {
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

    private static RuleResult r99(Registers registers) {
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

    private static RuleResult r100(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3express-control." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r101(Registers registers) {
        return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
    }

    private static RuleResult r102(Registers registers) {
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

    private static RuleResult r103(Registers registers) {
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

    private static RuleResult r104(Registers registers) {
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

    private static RuleResult r105(Registers registers) {
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

    private static RuleResult r106(Registers registers) {
        return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + registers.hardwareType
                                + "\"");
    }

    private static RuleResult r107(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
    }

    private static RuleResult r108(Registers registers) {
        return RuleResult.error("Custom endpoint `" + registers.endpoint + "` was not a valid URI");
    }

    private static RuleResult r109(Registers registers) {
        return RuleResult.error("S3 Accelerate cannot be used in this region");
    }

    private static RuleResult r110(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r111(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r112(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r113(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-fips." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r114(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r115(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate.dualstack."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r116(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack.us-east-1."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r117(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3.dualstack." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r118(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r119(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r120(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r121(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.bucket + "." + registers.url.authority()
                                                       + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r122(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r123(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r124(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r125(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r126(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.bucket + ".s3." + registers.region + "."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r127(Registers registers) {
        return RuleResult.error("Invalid region: region was not a valid DNS name.");
    }

    private static RuleResult r128(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support Dual-stack");
    }

    private static RuleResult r129(Registers registers) {
        return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
    }

    private static RuleResult r130(Registers registers) {
        return RuleResult.error("Access points are not supported for this operation");
    }

    private static RuleResult r131(Registers registers) {
        return RuleResult.error("Invalid configuration: region from ARN `" + registers.bucketArn.region()
                                + "` does not match client region `" + registers.region + "` and UseArnRegion is `false`");
    }

    private static RuleResult r132(Registers registers) {
        return RuleResult.error("Invalid ARN: Missing account id");
    }

    private static RuleResult r133(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult r134(Registers registers) {
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

    private static RuleResult r135(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-object-lambda." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult r136(Registers registers) {
        return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.accessPointName_ssa_1 + "`");
    }

    private static RuleResult r137(Registers registers) {
        return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.bucketArn.accountId() + "`");
    }

    private static RuleResult r138(Registers registers) {
        return RuleResult.error("Invalid region in ARN: `" + registers.bucketArn.region() + "` (invalid DNS name)");
    }

    private static RuleResult r139(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name() + "` but ARN (`"
                                + registers.bucket + "`) has `" + registers.bucketPartition.name() + "`");
    }

    private static RuleResult r140(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
    }

    private static RuleResult r141(Registers registers) {
        return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
    }

    private static RuleResult r142(Registers registers) {
        return RuleResult
            .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
    }

    private static RuleResult r143(Registers registers) {
        return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                + registers.arnType + "`");
    }

    private static RuleResult r144(Registers registers) {
        return RuleResult.error("Access Points do not support S3 Accelerate");
    }

    private static RuleResult r145(Registers registers) {
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

    private static RuleResult r146(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint-fips." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult r147(Registers registers) {
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

    private static RuleResult r148(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                       + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult r149(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                       + ".s3-accesspoint." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.bucketArn.region()).build())).build());
    }

    private static RuleResult r150(Registers registers) {
        return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + registers.bucketArn.service());
    }

    private static RuleResult r151(Registers registers) {
        return RuleResult.error("S3 MRAP does not support dual-stack");
    }

    private static RuleResult r152(Registers registers) {
        return RuleResult.error("S3 MRAP does not support FIPS");
    }

    private static RuleResult r153(Registers registers) {
        return RuleResult.error("S3 MRAP does not support S3 Accelerate");
    }

    private static RuleResult r154(Registers registers) {
        return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
    }

    private static RuleResult r155(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://" + registers.accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                       + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                         .signingRegionSet(Arrays.asList("*")).build())).build());
    }

    private static RuleResult r156(Registers registers) {
        return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                + "` but bucket referred to partition `" + registers.bucketArn.partition() + "`");
    }

    private static RuleResult r157(Registers registers) {
        return RuleResult.error("Invalid Access Point Name");
    }

    private static RuleResult r158(Registers registers) {
        return RuleResult.error("S3 Outposts does not support Dual-stack");
    }

    private static RuleResult r159(Registers registers) {
        return RuleResult.error("S3 Outposts does not support FIPS");
    }

    private static RuleResult r160(Registers registers) {
        return RuleResult.error("S3 Outposts does not support S3 Accelerate");
    }

    private static RuleResult r161(Registers registers) {
        return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
    }

    private static RuleResult r162(Registers registers) {
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

    private static RuleResult r163(Registers registers) {
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

    private static RuleResult r164(Registers registers) {
        return RuleResult.error("Expected an outpost type `accesspoint`, found " + registers.outpostType);
    }

    private static RuleResult r165(Registers registers) {
        return RuleResult.error("Invalid ARN: expected an access point name");
    }

    private static RuleResult r166(Registers registers) {
        return RuleResult.error("Invalid ARN: Expected a 4-component resource");
    }

    private static RuleResult r167(Registers registers) {
        return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                + registers.outpostId_ssa_1 + "`");
    }

    private static RuleResult r168(Registers registers) {
        return RuleResult.error("Invalid ARN: The Outpost Id was not set");
    }

    private static RuleResult r169(Registers registers) {
        return RuleResult.error("Invalid ARN: Unrecognized format: " + registers.bucket + " (type: " + registers.arnType + ")");
    }

    private static RuleResult r170(Registers registers) {
        return RuleResult.error("Invalid ARN: No ARN type specified");
    }

    private static RuleResult r171(Registers registers) {
        return RuleResult.error("Invalid ARN: `" + registers.bucket + "` was not a valid ARN");
    }

    private static RuleResult r172(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
    }

    private static RuleResult r173(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r174(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()
                                                       + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r175(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r176(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r177(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r178(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r179(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r180(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r181(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r182(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r183(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                       + registers.uri_encoded_bucket))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r184(Registers registers) {
        return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
    }

    private static RuleResult r185(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r186(Registers registers) {
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

    private static RuleResult r187(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-object-lambda." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r188(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r189(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r190(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r191(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3-fips." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r192(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r193(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3.dualstack." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r194(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r195(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r196(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion("us-east-1").build())).build());
    }

    private static RuleResult r197(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r198(Registers registers) {
        return RuleResult.endpoint(Endpoint
                                       .builder()
                                       .url(URI.create("https://s3." + registers.region + "." + registers.partitionResult.dnsSuffix()))
                                       .putAttribute(
                                           AwsEndpointAttribute.AUTH_SCHEMES,
                                           Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                        .signingRegion(registers.region).build())).build());
    }

    private static RuleResult r199(Registers registers) {
        return RuleResult.error("A region must be set when sending requests to S3.");
    }

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        try {
            Validate.notNull(params.useFips(), "Parameter 'UseFIPS' must not be null");
            Validate.notNull(params.useDualStack(), "Parameter 'UseDualStack' must not be null");
            Validate.notNull(params.forcePathStyle(), "Parameter 'ForcePathStyle' must not be null");
            Validate.notNull(params.accelerate(), "Parameter 'Accelerate' must not be null");
            Validate.notNull(params.useGlobalEndpoint(), "Parameter 'UseGlobalEndpoint' must not be null");
            Validate.notNull(params.disableMultiRegionAccessPoints(),
                             "Parameter 'DisableMultiRegionAccessPoints' must not be null");
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
            int nodeRef = 2;
            while (nodeRef != 1 && nodeRef != -1 && nodeRef < 100000000) {
                boolean complemented = nodeRef < 0;
                int nodeI = java.lang.Math.abs(nodeRef) - 1;
                boolean conditionResult = (boolean) CONDITION_FNS[BDD_DEFINITION[nodeI * 3]].invokeExact(registers);
                if (complemented == conditionResult) {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 2];
                } else {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 1];
                }
            }
            if (nodeRef == -1 || nodeRef == 1) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            } else {
                RuleResult result = (RuleResult) RESULT_FNS[nodeRef - 100000001].invokeExact(registers);
                if (result.isError()) {
                    String errorMsg = result.error();
                    if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                        errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                    }
                    throw SdkClientException.create(errorMsg);
                }
                return CompletableFuture.completedFuture(result.endpoint());
            }
        } catch (Throwable error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private static class Registers {
        String bucket;

        String region;

        Boolean useFIPS;

        Boolean useDualStack;

        String endpoint;

        Boolean forcePathStyle;

        Boolean accelerate;

        Boolean useGlobalEndpoint;

        Boolean useObjectLambdaEndpoint;

        String key;

        String prefix;

        String copySource;

        Boolean disableAccessPoints;

        Boolean disableMultiRegionAccessPoints;

        Boolean useArnRegion;

        Boolean useS3ExpressControlEndpoint;

        Boolean disableS3ExpressSessionAuth;

        RulePartition partitionResult;

        RuleUrl url;

        RuleArn bucketArn;

        String outpostId_ssa_2;

        String hardwareType;

        String accessPointSuffix;

        String regionPrefix;

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

        RulePartition bucketPartition;

        String outpostId_ssa_1;

        String accessPointName_ssa_1;

        String outpostType;

        String accessPointName_ssa_2;
    }
}
