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
import software.amazon.awssdk.utils.Validate;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class BddOptimizedRuntimeWithSwitches implements S3EndpointProvider {

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
            registers.Region = params.region() == null ? null : params.region().id();
            registers.Bucket = params.bucket();
            registers.UseFIPS = params.useFips();
            registers.UseDualStack = params.useDualStack();
            registers.Endpoint = params.endpoint();
            registers.ForcePathStyle = params.forcePathStyle();
            registers.Accelerate = params.accelerate();
            registers.UseGlobalEndpoint = params.useGlobalEndpoint();
            registers.UseObjectLambdaEndpoint = params.useObjectLambdaEndpoint();
            registers.Key = params.key();
            registers.Prefix = params.prefix();
            registers.CopySource = params.copySource();
            registers.DisableAccessPoints = params.disableAccessPoints();
            registers.DisableMultiRegionAccessPoints = params.disableMultiRegionAccessPoints();
            registers.UseArnRegion = params.useArnRegion();
            registers.UseS3ExpressControlEndpoint = params.useS3ExpressControlEndpoint();
            registers.DisableS3ExpressSessionAuth = params.disableS3ExpressSessionAuth();
            int nodeRef = 2;
            while (nodeRef != 1 && nodeRef != -1 && nodeRef < 100000000) {
                boolean complemented = nodeRef < 0;
                int nodeI = java.lang.Math.abs(nodeRef) - 1;
                boolean conditionResult = evaluateCondition(BDD_DEFINITION[nodeI * 3], registers);
                if (complemented == conditionResult) {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 2];
                } else {
                    nodeRef = BDD_DEFINITION[nodeI * 3 + 1];
                }
            }
            if (nodeRef == -1 || nodeRef == 1) {
                throw SdkClientException.create("Rule engine did not reach an error or endpoint result");
            } else {
                RuleResult result = evaluateResult(nodeRef - 100000001, registers);
                if (result.isError()) {
                    String errorMsg = result.error();
                    if (errorMsg.contains("Invalid ARN") && errorMsg.contains(":s3:::")) {
                        errorMsg += ". Use the bucket name instead of simple bucket ARNs in GetBucketLocationRequest.";
                    }
                    throw SdkClientException.create(errorMsg);
                }
                return CompletableFuture.completedFuture(result.endpoint());
            }
        } catch (Exception error) {
            return CompletableFutureUtils.failedFuture(error);
        }
    }

    private boolean evaluateCondition(int conditionIndex, Registers registers) {
        switch (conditionIndex) {
            case 0:
                return (registers.Region != null);
            case 1:
                return (registers.Accelerate);
            case 2:
                return (registers.UseFIPS);
            case 3:
                return (registers.Endpoint != null);
            case 4:
                return (registers.Bucket != null);
            case 5:
                registers.partitionResult = RulesFunctions.awsPartition(registers.Region);
                return registers.partitionResult != null;
            case 6:
                return ("aws-cn".equals(registers.partitionResult.name()));
            case 7:
                return (registers.UseS3ExpressControlEndpoint != null);
            case 8:
                return (registers.UseS3ExpressControlEndpoint);
            case 9:
                return (registers.DisableS3ExpressSessionAuth != null);
            case 10:
                return (registers.DisableS3ExpressSessionAuth);
            case 11:
                return (RulesFunctions.awsParseArn(registers.Bucket) != null);
            case 12:
                return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 0, 6, true), "")));
            case 13:
                return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 0, 7, true), "")));
            case 14:
                return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 0, 4, false), "")));
            case 15:
                return (RulesFunctions.parseURL(registers.Endpoint) != null);
            case 16:
                registers.url = RulesFunctions.parseURL(registers.Endpoint);
                return registers.url != null;
            case 17:
                return (registers.ForcePathStyle);
            case 18:
                return (RulesFunctions.awsParseArn(registers.Bucket) != null);
            case 19:
                return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.Bucket, false));
            case 20:
                return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.Bucket, true));
            case 21:
                return ("http".equals(registers.url.scheme()));
            case 22:
                registers.bucketArn = RulesFunctions.awsParseArn(registers.Bucket);
                return registers.bucketArn != null;
            case 23:
                registers.outpostId_ssa_2 = RulesFunctions.substring(registers.Bucket, 32, 49, true);
                return registers.outpostId_ssa_2 != null;
            case 24:
                registers.hardwareType = RulesFunctions.substring(registers.Bucket, 49, 50, true);
                return registers.hardwareType != null;
            case 25:
                registers.accessPointSuffix = RulesFunctions.substring(registers.Bucket, 0, 7, true);
                return registers.accessPointSuffix != null;
            case 26:
                return ("--op-s3".equals(registers.accessPointSuffix));
            case 27:
                registers.regionPrefix = RulesFunctions.substring(registers.Bucket, 8, 12, true);
                return registers.regionPrefix != null;
            case 28:
                registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.Bucket);
                return registers.uri_encoded_bucket != null;
            case 29:
                return (registers.UseObjectLambdaEndpoint != null);
            case 30:
                return (registers.UseObjectLambdaEndpoint);
            case 31:
                return (RulesFunctions.isValidHostLabel(registers.Region, true));
            case 32:
                return ("beta".equals(registers.regionPrefix));
            case 33:
                return (RulesFunctions.isValidHostLabel(registers.Region, false));
            case 34:
                return (registers.url.isIp());
            case 35:
                return (registers.UseDualStack);
            case 36:
                return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
            case 37:
                registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.Bucket, 7, 15, true);
                return registers.s3expressAvailabilityZoneId_ssa_6 != null;
            case 38:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 15, 17, true), "")));
            case 39:
                return ("e".equals(registers.hardwareType));
            case 40:
                return ("o".equals(registers.hardwareType));
            case 41:
                registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.Bucket, 6, 15, true);
                return registers.s3expressAvailabilityZoneId_ssa_2 != null;
            case 42:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 16, 18, true), "")));
            case 43:
                registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.Bucket, 7, 16, true);
                return registers.s3expressAvailabilityZoneId_ssa_7 != null;
            case 44:
                return (registers.DisableAccessPoints != null);
            case 45:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 14, 16, true), "")));
            case 46:
                registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.Bucket, 6, 14, true);
                return registers.s3expressAvailabilityZoneId_ssa_1 != null;
            case 47:
                registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.Bucket, 7, 20, true);
                return registers.s3expressAvailabilityZoneId_ssa_8 != null;
            case 48:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 19, 21, true), "")));
            case 49:
                return (registers.DisableAccessPoints);
            case 50:
                registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.Bucket, 6, 19, true);
                return registers.s3expressAvailabilityZoneId_ssa_3 != null;
            case 51:
                registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.Bucket, 6, 20, true);
                return registers.s3expressAvailabilityZoneId_ssa_4 != null;
            case 52:
                return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
            case 53:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 20, 22, true), "")));
            case 54:
                return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
            case 55:
                registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
                return registers.arnType != null;
            case 56:
                return ("accesspoint".equals(registers.arnType));
            case 57:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 21, 23, true), "")));
            case 58:
                registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.Bucket, 7, 21, true);
                return registers.s3expressAvailabilityZoneId_ssa_9 != null;
            case 59:
                return ("".equals(registers.arnType));
            case 60:
                registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.Bucket, 6, 26, true);
                return registers.s3expressAvailabilityZoneId_ssa_5 != null;
            case 61:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 26, 28, true), "")));
            case 62:
                registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.Bucket, 7, 27, true);
                return registers.s3expressAvailabilityZoneId_ssa_10 != null;
            case 63:
                return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.Bucket, 27, 29, true), "")));
            case 64:
                return ("".equals(registers.bucketArn.region()));
            case 65:
                registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
                return registers.bucketPartition != null;
            case 66:
                return ("s3-object-lambda".equals(registers.bucketArn.service()));
            case 67:
                return ("s3-outposts".equals(registers.bucketArn.service()));
            case 68:
                registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
                return registers.outpostId_ssa_1 != null;
            case 69:
                return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
            case 70:
                registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
                return registers.accessPointName_ssa_1 != null;
            case 71:
                return ("".equals(registers.accessPointName_ssa_1));
            case 72:
                return (registers.UseArnRegion != null);
            case 73:
                return (!registers.UseArnRegion);
            case 74:
                return (RulesFunctions.stringEquals(registers.Region, registers.bucketArn.region()));
            case 75:
                return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
            case 76:
                return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
            case 77:
                return ("".equals(registers.bucketArn.accountId()));
            case 78:
                return ("s3".equals(registers.bucketArn.service()));
            case 79:
                return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
            case 80:
                registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
                return registers.outpostType != null;
            case 81:
                registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
                return registers.accessPointName_ssa_2 != null;
            case 82:
                return ("accesspoint".equals(registers.outpostType));
            case 83:
                return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
            case 84:
                return (registers.DisableMultiRegionAccessPoints);
            case 85:
                return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
            case 86:
                return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
            case 87:
                return (!registers.url.isIp());
            case 88:
                return ("aws-global".equals(registers.Region));
            case 89:
                return ("us-east-1".equals(registers.Region));
            case 90:
                return (registers.UseGlobalEndpoint);
            default:
                throw new IllegalArgumentException("Unknown condition index");
        }
    }

    private RuleResult evaluateResult(int resultIndex, Registers registers) {
        switch (resultIndex) {
            case 0:
                return RuleResult.error("Accelerate cannot be used with FIPS");
            case 1:
                return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
            case 2:
                return RuleResult.error("A custom endpoint cannot be combined with FIPS");
            case 3:
                return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
            case 4:
                return RuleResult.error("Partition does not support FIPS");
            case 5:
                return RuleResult.error("S3Express does not support S3 Accelerate.");
            case 6:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/"
                                                                         + registers.uri_encoded_bucket + registers.url.path()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 7:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.Bucket + "." + registers.url.authority()
                                                                         + registers.url.path()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 8:
                return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
            case 9:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + "/"
                                                                         + registers.uri_encoded_bucket + registers.url.path()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 10:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.Bucket + "." + registers.url.authority()
                                                                         + registers.url.path()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 11:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control-fips.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 12:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control-fips." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 13:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 14:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control." + registers.Region + "." + registers.partitionResult.dnsSuffix()
                                                                         + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 15:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 16:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 17:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 18:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 19:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 20:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 21:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 22:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 23:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 24:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 25:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 26:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 27:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 28:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 29:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 30:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 31:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 32:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 33:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 34:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 35:
                return RuleResult.error("Unrecognized S3Express bucket name format.");
            case 36:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_1 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 37:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_1 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 38:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 39:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_1
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 40:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_2 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 41:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_2 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 42:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 43:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_2
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 44:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_3 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 45:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_3 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 46:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 47:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_3
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 48:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_4 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 49:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_4 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 50:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 51:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_4
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 52:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_5 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 53:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_5 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 54:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 55:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_5
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 56:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 57:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 58:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 59:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 60:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 61:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 62:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 63:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 64:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 65:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 66:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 67:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 68:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 69:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 70:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 71:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 72:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 73:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 74:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 75:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 76:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_6 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 77:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_6 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 78:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 79:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_6
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 80:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_7 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 81:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_7 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 82:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 83:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_7
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 84:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_8 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 85:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_8 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 86:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 87:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_8
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 88:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_9 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 89:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_9 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 90:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 91:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_9
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 92:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_10 + ".dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 93:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-fips-"
                                                                         + registers.s3expressAvailabilityZoneId_ssa_10 + "." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 94:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                                         + ".dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 95:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3express-" + registers.s3expressAvailabilityZoneId_ssa_10
                                                                         + "." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(S3ExpressEndpointAuthScheme.builder().disableDoubleEncoding(true)
                                                                                            .signingName("s3express").signingRegion(registers.Region).build())).build());
            case 96:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 97:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control-fips.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 98:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control-fips." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 99:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3express-control.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                .signingRegion(registers.Region).build())).build());
            case 100:
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(URI.create("https://s3express-control." + registers.Region + "."
                                                            + registers.partitionResult.dnsSuffix()))
                                  .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                   .signingRegion(registers.Region).build())).build());
            case 101:
                return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
            case 102:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".ec2." + registers.url.authority()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                               .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.Region)
                                                                                                                                               .build())).build());
            case 103:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".ec2.s3-outposts." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                               .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.Region)
                                                                                                                                               .build())).build());
            case 104:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".op-" + registers.outpostId_ssa_2 + "."
                                                                         + registers.url.authority()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                               .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.Region)
                                                                                                                                               .build())).build());
            case 105:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".op-" + registers.outpostId_ssa_2 + ".s3-outposts."
                                                                         + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                               .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(registers.Region)
                                                                                                                                               .build())).build());
            case 106:
                return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got "
                                        + registers.hardwareType + "\"");
            case 107:
                return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
            case 108:
                return RuleResult.error("Custom endpoint `" + registers.Endpoint + "` was not a valid URI");
            case 109:
                return RuleResult.error("S3 Accelerate cannot be used in this region");
            case 110:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-fips.dualstack.us-east-1."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 111:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-fips.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 112:
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(URI.create("https://" + registers.Bucket + ".s3-fips.us-east-1."
                                                            + registers.partitionResult.dnsSuffix()))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                   .signingRegion("us-east-1").build())).build());
            case 113:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-fips." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 114:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-accelerate.dualstack.us-east-1."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 115:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-accelerate.dualstack."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 116:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3.dualstack.us-east-1."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 117:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3.dualstack." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 118:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                                         + registers.Bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 119:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.Bucket + "." + registers.url.authority()
                                                                         + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 120:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                                         + registers.Bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 121:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.Bucket + "." + registers.url.authority()
                                                                         + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 122:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 123:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3-accelerate." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 124:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 125:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 126:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.Bucket + ".s3." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 127:
                return RuleResult.error("Invalid region: region was not a valid DNS name.");
            case 128:
                return RuleResult.error("S3 Object Lambda does not support Dual-stack");
            case 129:
                return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
            case 130:
                return RuleResult.error("Access points are not supported for this operation");
            case 131:
                return RuleResult.error("Invalid configuration: region from ARN `" + registers.bucketArn.region()
                                        + "` does not match client region `" + registers.Region + "` and UseArnRegion is `false`");
            case 132:
                return RuleResult.error("Invalid ARN: Missing account id");
            case 133:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                                         + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 134:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-object-lambda-fips." + registers.bucketArn.region() + "."
                                                                         + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 135:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-object-lambda." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 136:
                return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                        + registers.accessPointName_ssa_1 + "`");
            case 137:
                return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                        + registers.bucketArn.accountId() + "`");
            case 138:
                return RuleResult.error("Invalid region in ARN: `" + registers.bucketArn.region() + "` (invalid DNS name)");
            case 139:
                return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name() + "` but ARN (`"
                                        + registers.Bucket + "`) has `" + registers.bucketPartition.name() + "`");
            case 140:
                return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
            case 141:
                return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
            case 142:
                return RuleResult
                    .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
            case 143:
                return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                        + registers.arnType + "`");
            case 144:
                return RuleResult.error("Access Points do not support S3 Accelerate");
            case 145:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-accesspoint-fips.dualstack." + registers.bucketArn.region() + "."
                                                                         + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 146:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-accesspoint-fips." + registers.bucketArn.region() + "."
                                                                         + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 147:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-accesspoint.dualstack." + registers.bucketArn.region() + "."
                                                                         + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 148:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.accessPointName_ssa_1 + "-"
                                                                         + registers.bucketArn.accountId() + "." + registers.url.authority() + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 149:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + "-" + registers.bucketArn.accountId()
                                                                         + ".s3-accesspoint." + registers.bucketArn.region() + "." + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 150:
                return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + registers.bucketArn.service());
            case 151:
                return RuleResult.error("S3 MRAP does not support dual-stack");
            case 152:
                return RuleResult.error("S3 MRAP does not support FIPS");
            case 153:
                return RuleResult.error("S3 MRAP does not support S3 Accelerate");
            case 154:
                return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
            case 155:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                 .signingRegionSet(Arrays.asList("*")).build())).build());
            case 156:
                return RuleResult.error("Client was configured for partition `" + registers.partitionResult.name()
                                        + "` but bucket referred to partition `" + registers.bucketArn.partition() + "`");
            case 157:
                return RuleResult.error("Invalid Access Point Name");
            case 158:
                return RuleResult.error("S3 Outposts does not support Dual-stack");
            case 159:
                return RuleResult.error("S3 Outposts does not support FIPS");
            case 160:
                return RuleResult.error("S3 Outposts does not support S3 Accelerate");
            case 161:
                return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
            case 162:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                                         + registers.outpostId_ssa_1 + "." + registers.url.authority()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(),
                                                                 SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 163:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://" + registers.accessPointName_ssa_2 + "-" + registers.bucketArn.accountId() + "."
                                                                         + registers.outpostId_ssa_1 + ".s3-outposts." + registers.bucketArn.region() + "."
                                                                         + registers.bucketPartition.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                 .signingRegionSet(Arrays.asList("*")).build(),
                                                                 SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                .signingRegion(registers.bucketArn.region()).build())).build());
            case 164:
                return RuleResult.error("Expected an outpost type `accesspoint`, found " + registers.outpostType);
            case 165:
                return RuleResult.error("Invalid ARN: expected an access point name");
            case 166:
                return RuleResult.error("Invalid ARN: Expected a 4-component resource");
            case 167:
                return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                        + registers.outpostId_ssa_1 + "`");
            case 168:
                return RuleResult.error("Invalid ARN: The Outpost Id was not set");
            case 169:
                return RuleResult.error("Invalid ARN: Unrecognized format: " + registers.Bucket + " (type: " + registers.arnType
                                        + ")");
            case 170:
                return RuleResult.error("Invalid ARN: No ARN type specified");
            case 171:
                return RuleResult.error("Invalid ARN: `" + registers.Bucket + "` was not a valid ARN");
            case 172:
                return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
            case 173:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 174:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips.dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()
                                                                         + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 175:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 176:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips." + registers.Region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 177:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix() + "/"
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 178:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3.dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()
                                                                         + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 179:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 180:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.normalizedPath()
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 181:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 182:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix() + "/" + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 183:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.Region + "." + registers.partitionResult.dnsSuffix() + "/"
                                                                         + registers.uri_encoded_bucket))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 184:
                return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
            case 185:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                                .signingRegion(registers.Region).build())).build());
            case 186:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-object-lambda-fips." + registers.Region + "."
                                                                         + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                                .signingRegion(registers.Region).build())).build());
            case 187:
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(URI.create("https://s3-object-lambda." + registers.Region + "."
                                                            + registers.partitionResult.dnsSuffix()))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                   .signingName("s3-object-lambda").signingRegion(registers.Region).build())).build());
            case 188:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 189:
                return RuleResult
                    .endpoint(Endpoint
                                  .builder()
                                  .url(URI.create("https://s3-fips.dualstack." + registers.Region + "."
                                                            + registers.partitionResult.dnsSuffix()))
                                  .putAttribute(
                                      AwsEndpointAttribute.AUTH_SCHEMES,
                                      Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                   .signingRegion(registers.Region).build())).build());
            case 190:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips.us-east-1." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 191:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3-fips." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 192:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3.dualstack.us-east-1." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 193:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3.dualstack." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 194:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 195:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create(registers.url.scheme() + "://" + registers.url.authority() + registers.url.path()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 196:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion("us-east-1").build())).build());
            case 197:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 198:
                return RuleResult.endpoint(Endpoint
                                               .builder()
                                               .url(URI.create("https://s3." + registers.Region + "." + registers.partitionResult.dnsSuffix()))
                                               .putAttribute(
                                                   AwsEndpointAttribute.AUTH_SCHEMES,
                                                   Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                .signingRegion(registers.Region).build())).build());
            case 199:
                return RuleResult.error("A region must be set when sending requests to S3.");
            default:
                throw new IllegalArgumentException("Unknown result index");
        }
    }

    private static class Registers {
        String Bucket;

        String Region;

        Boolean UseFIPS;

        Boolean UseDualStack;

        String Endpoint;

        Boolean ForcePathStyle;

        Boolean Accelerate;

        Boolean UseGlobalEndpoint;

        Boolean UseObjectLambdaEndpoint;

        String Key;

        String Prefix;

        String CopySource;

        Boolean DisableAccessPoints;

        Boolean DisableMultiRegionAccessPoints;

        Boolean UseArnRegion;

        Boolean UseS3ExpressControlEndpoint;

        Boolean DisableS3ExpressSessionAuth;

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
