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
public final class BddCostOpt2Runtime4 implements S3EndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1,
                                                  0, 3, 100000200,
                                                  4, 828, 4,
                                                  5, 569, 5,
                                                  1, 163, 6, 2, 12, 7, 19, 8,
                                                  100000200, 3, 9, 10, 8, 842, 10, 15, 11, 836, 17, 127, 836, 19, 24, 13, 20, 19, 14, 79, 100000009, 15, 80, 100000009,
                                                  16, 81, 17, 21, 10, 100000172, 18, 36, 23, 100000172, 79, 100000009, 20, 80, 100000009, 21, 10, 869, 22, 36, 23,
                                                  100000200, 27, 862, 200, 3, 25, 26, 8, 36, 26, 7, 27, 28, 9, 32, 28, 20, 30, 29, 79, 31, 40, 79, 31, 42, 12, 57,
                                                  100000009, 20, 34, 33, 79, 35, 72, 79, 35, 78, 12, 149, 100000009, 7, 37, 38, 9, 70, 38, 20, 41, 39, 79, 55, 40, 80,
                                                  43, 73, 79, 55, 42, 80, 43, 79, 12, 44, 100000009, 27, 45, 241, 30, 46, 47, 82, 100000079, 47, 83, 48, 49, 32,
                                                  100000083, 49, 45, 50, 51, 86, 100000087, 51, 87, 52, 53, 53, 100000091, 53, 55, 54, 100000036, 89, 100000095,
                                                  100000036, 12, 56, 147, 37, 162, 57, 27, 58, 254, 82, 59, 60, 31, 68, 60, 84, 61, 62, 44, 100000039, 62, 85, 63, 64,
                                                  46, 100000047, 64, 47, 65, 66, 86, 100000051, 66, 54, 67, 100000036, 88, 100000055, 100000036, 84, 69, 100000043, 44,
                                                  100000039, 100000043, 20, 77, 71, 79, 146, 72, 80, 134, 73, 81, 74, 79, 10, 897, 75, 12, 107, 76, 36, 82, 897, 79,
                                                  146, 78, 80, 134, 79, 10, 118, 80, 12, 107, 81, 36, 82, 119, 21, 83, 87, 22, 84, 87, 23, 85, 87, 16, 86, 87, 24,
                                                  1012, 87, 27, 88, 323, 26, 89, 90, 40, 974, 90, 56, 967, 91, 41, 92, 100000171, 50, 93, 976, 51, 100000171, 94, 57,
                                                  984, 95, 78, 96, 950, 58, 988, 97, 49, 98, 100000143, 52, 100000143, 99, 39, 100, 102, 42, 101, 102, 61, 102,
                                                  100000132, 62, 103, 100000140, 75, 104, 100000139, 72, 105, 100000151, 76, 106, 100000138, 73, 100000148, 100000137,
                                                  21, 108, 112, 22, 109, 112, 23, 110, 112, 16, 111, 112, 24, 1012, 112, 34, 113, 100000128, 27, 117, 114, 68,
                                                  100000125, 115, 69, 116, 100000127, 64, 100000126, 100000127, 68, 100000117, 100000118, 11, 1007, 119, 21, 120, 124,
                                                  22, 121, 124, 23, 122, 124, 16, 123, 124, 24, 1012, 124, 37, 129, 125, 15, 126, 100000200, 17, 127, 100000200, 33,
                                                  128, 100000128, 27, 100000129, 100000188, 27, 133, 130, 68, 100000182, 131, 69, 132, 100000184, 64, 100000183,
                                                  100000184, 68, 100000178, 100000179, 12, 135, 100000009, 27, 136, 536, 30, 137, 138, 82, 100000059, 138, 83, 139,
                                                  140, 32, 100000063, 140, 45, 141, 142, 86, 100000067, 142, 87, 143, 144, 53, 100000071, 144, 55, 145, 100000036, 89,
                                                  100000075, 100000036, 12, 148, 147, 37, 162, 100000009, 37, 162, 149, 27, 150, 549, 82, 151, 152, 31, 160, 152, 84,
                                                  153, 154, 44, 100000018, 154, 85, 155, 156, 46, 100000026, 156, 47, 157, 158, 86, 100000030, 158, 54, 159, 100000036,
                                                  88, 100000034, 100000036, 84, 161, 100000022, 44, 100000018, 100000022, 27, 100000014, 100000015, 2, 178, 164, 19,
                                                  165, 515, 3, 166, 167, 8, 175, 167, 38, 170, 168, 15, 169, 517, 17, 390, 517, 15, 171, 172, 17, 516, 172, 33, 173,
                                                  517, 27, 100000002, 174, 68, 100000195, 100000196, 38, 177, 176, 27, 100000002, 100000101, 27, 100000002, 100000097,
                                                  19, 231, 179, 7, 180, 181, 9, 187, 181, 20, 184, 182, 79, 186, 183, 80, 186, 190, 79, 186, 185, 80, 186, 196, 38,
                                                  266, 564, 20, 194, 188, 79, 230, 189, 80, 230, 190, 81, 191, 196, 90, 192, 312, 10, 297, 193, 36, 199, 297, 79, 230,
                                                  195, 80, 230, 196, 90, 197, 312, 10, 229, 198, 36, 199, 515, 27, 100000002, 200, 26, 201, 202, 40, 217, 202, 48, 208,
                                                  203, 56, 206, 204, 41, 205, 100000171, 50, 211, 220, 41, 207, 100000171, 50, 215, 220, 56, 213, 209, 41, 210,
                                                  100000171, 50, 211, 475, 51, 100000171, 212, 57, 225, 951, 41, 214, 100000171, 50, 215, 475, 51, 100000171, 216, 57,
                                                  225, 972, 48, 221, 218, 41, 219, 100000171, 50, 223, 220, 51, 100000171, 453, 41, 222, 100000171, 50, 223, 475, 51,
                                                  100000171, 224, 57, 225, 982, 58, 732, 226, 49, 227, 100000143, 52, 100000143, 228, 63, 486, 100000158, 11, 526, 515,
                                                  38, 561, 564, 7, 232, 233, 9, 273, 233, 20, 236, 234, 79, 251, 235, 80, 238, 276, 79, 251, 237, 80, 238, 300, 38,
                                                  266, 239, 12, 240, 564, 27, 100000002, 241, 30, 242, 243, 82, 100000080, 243, 83, 244, 245, 32, 100000084, 245, 45,
                                                  246, 247, 86, 100000088, 247, 87, 248, 249, 53, 100000092, 249, 55, 250, 100000036, 89, 100000096, 100000036, 38,
                                                  266, 252, 12, 253, 564, 27, 100000002, 254, 82, 255, 256, 31, 264, 256, 84, 257, 258, 44, 100000040, 258, 85, 259,
                                                  260, 46, 100000048, 260, 47, 261, 262, 86, 100000052, 262, 54, 263, 100000036, 88, 100000056, 100000036, 84, 265,
                                                  100000044, 44, 100000040, 100000044, 12, 269, 267, 37, 268, 564, 25, 272, 564, 37, 270, 271, 25, 272, 271, 27,
                                                  100000002, 100000011, 27, 100000002, 100000010, 20, 298, 274, 79, 546, 275, 80, 533, 276, 81, 277, 300, 90, 278, 301,
                                                  38, 287, 279, 10, 282, 280, 12, 376, 281, 36, 317, 282, 21, 283, 297, 22, 284, 297, 23, 285, 297, 16, 286, 297, 24,
                                                  528, 297, 10, 292, 288, 12, 494, 289, 13, 290, 291, 14, 487, 291, 36, 403, 292, 21, 293, 297, 22, 294, 297, 23, 295,
                                                  297, 16, 296, 297, 24, 527, 297, 27, 100000002, 100000172, 79, 546, 299, 80, 533, 300, 90, 313, 301, 38, 307, 302,
                                                  21, 303, 312, 22, 304, 312, 23, 305, 312, 16, 306, 312, 24, 528, 312, 21, 308, 312, 22, 309, 312, 23, 310, 312, 16,
                                                  311, 312, 24, 527, 312, 27, 100000002, 100000109, 38, 398, 314, 10, 381, 315, 12, 376, 316, 36, 317, 382, 21, 318,
                                                  322, 22, 319, 322, 23, 320, 322, 16, 321, 322, 24, 528, 322, 27, 100000002, 323, 26, 324, 325, 40, 358, 325, 48, 331,
                                                  326, 56, 329, 327, 41, 328, 100000171, 50, 334, 361, 41, 330, 100000171, 50, 446, 361, 56, 444, 332, 41, 333,
                                                  100000171, 50, 334, 475, 51, 100000171, 335, 57, 480, 336, 78, 337, 951, 58, 348, 338, 49, 339, 100000143, 52,
                                                  100000143, 340, 39, 341, 343, 42, 342, 343, 61, 343, 100000132, 62, 344, 100000140, 75, 345, 100000139, 72, 346,
                                                  100000151, 76, 347, 100000138, 73, 100000150, 100000137, 49, 349, 100000143, 52, 100000143, 350, 39, 351, 353, 42,
                                                  352, 353, 61, 353, 100000132, 62, 354, 100000140, 75, 355, 100000139, 71, 100000133, 356, 76, 357, 100000138, 73,
                                                  100000136, 100000137, 48, 473, 359, 41, 360, 100000171, 50, 478, 361, 51, 100000171, 362, 78, 363, 453, 58,
                                                  100000144, 364, 59, 365, 100000170, 43, 366, 100000169, 60, 367, 100000168, 39, 368, 370, 42, 369, 370, 61, 370,
                                                  100000132, 62, 371, 100000140, 75, 372, 100000139, 76, 373, 100000138, 65, 374, 100000167, 66, 375, 100000166, 67,
                                                  100000164, 100000165, 21, 377, 517, 22, 378, 517, 23, 379, 517, 16, 380, 517, 24, 528, 517, 11, 393, 382, 21, 383,
                                                  387, 22, 384, 387, 23, 385, 387, 16, 386, 387, 24, 528, 387, 37, 392, 388, 15, 389, 515, 17, 390, 515, 33, 391, 517,
                                                  27, 100000002, 100000188, 27, 100000002, 100000185, 21, 394, 526, 22, 395, 526, 23, 396, 526, 16, 397, 526, 24, 528,
                                                  526, 10, 506, 399, 12, 494, 400, 13, 401, 402, 14, 487, 402, 36, 403, 507, 21, 404, 408, 22, 405, 408, 23, 406, 408,
                                                  16, 407, 408, 24, 527, 408, 27, 100000002, 409, 26, 410, 411, 40, 448, 411, 48, 417, 412, 56, 415, 413, 41, 414,
                                                  100000171, 50, 420, 451, 41, 416, 100000171, 50, 446, 451, 56, 444, 418, 41, 419, 100000171, 50, 420, 475, 51,
                                                  100000171, 421, 57, 480, 422, 78, 423, 951, 58, 434, 424, 49, 425, 100000143, 52, 100000143, 426, 39, 427, 429, 42,
                                                  428, 429, 61, 429, 100000132, 62, 430, 100000140, 75, 431, 100000139, 72, 432, 100000151, 76, 433, 100000138, 73,
                                                  100000149, 100000137, 49, 435, 100000143, 52, 100000143, 436, 39, 437, 439, 42, 438, 439, 61, 439, 100000132, 62,
                                                  440, 100000140, 75, 441, 100000139, 71, 100000133, 442, 76, 443, 100000138, 73, 100000134, 100000137, 41, 445,
                                                  100000171, 50, 446, 475, 51, 100000171, 447, 57, 480, 972, 48, 473, 449, 41, 450, 100000171, 50, 478, 451, 51,
                                                  100000171, 452, 78, 460, 453, 58, 100000144, 454, 59, 455, 100000170, 43, 456, 100000169, 60, 457, 100000168, 39,
                                                  458, 100000168, 42, 459, 100000168, 61, 100000168, 100000132, 58, 100000144, 461, 59, 462, 100000170, 43, 463,
                                                  100000169, 60, 464, 100000168, 39, 465, 467, 42, 466, 467, 61, 467, 100000132, 62, 468, 100000140, 75, 469,
                                                  100000139, 76, 470, 100000138, 65, 471, 100000167, 66, 472, 100000166, 67, 100000163, 100000165, 41, 474, 100000171,
                                                  50, 478, 475, 51, 100000171, 476, 58, 100000144, 477, 59, 100000162, 100000170, 51, 100000171, 479, 57, 480, 982, 58,
                                                  732, 481, 49, 482, 100000143, 52, 100000143, 483, 63, 486, 484, 74, 485, 100000158, 77, 100000156, 100000157, 74,
                                                  100000155, 100000158, 21, 488, 492, 22, 489, 492, 23, 490, 492, 16, 491, 492, 24, 527, 492, 34, 493, 517, 27,
                                                  100000002, 100000122, 21, 495, 499, 22, 496, 499, 23, 497, 499, 16, 498, 499, 24, 527, 499, 34, 500, 517, 25, 504,
                                                  501, 27, 100000002, 502, 70, 503, 100000128, 68, 100000120, 100000122, 27, 100000002, 505, 68, 100000119, 100000121,
                                                  11, 521, 507, 21, 508, 512, 22, 509, 512, 23, 510, 512, 16, 511, 512, 24, 527, 512, 37, 519, 513, 15, 514, 515, 17,
                                                  516, 515, 27, 100000002, 100000200, 33, 518, 517, 27, 100000002, 100000128, 27, 100000002, 100000186, 27, 100000002,
                                                  520, 68, 100000180, 100000181, 21, 522, 526, 22, 523, 526, 23, 524, 526, 16, 525, 526, 24, 527, 526, 27, 100000002,
                                                  100000173, 18, 529, 528, 27, 100000002, 1013, 27, 100000002, 530, 35, 531, 100000108, 28, 100000103, 532, 29,
                                                  100000105, 100000107, 38, 561, 534, 12, 535, 564, 27, 100000002, 536, 30, 537, 538, 82, 100000060, 538, 83, 539, 540,
                                                  32, 100000064, 540, 45, 541, 542, 86, 100000068, 542, 87, 543, 544, 53, 100000072, 544, 55, 545, 100000036, 89,
                                                  100000076, 100000036, 38, 561, 547, 12, 548, 564, 27, 100000002, 549, 82, 550, 551, 31, 559, 551, 84, 552, 553, 44,
                                                  100000019, 553, 85, 554, 555, 46, 100000027, 555, 47, 556, 557, 86, 100000031, 557, 54, 558, 100000036, 88,
                                                  100000035, 100000036, 84, 560, 100000023, 44, 100000019, 100000023, 12, 565, 562, 37, 563, 564, 25, 568, 564, 27,
                                                  100000002, 100000009, 37, 566, 567, 25, 568, 567, 27, 100000002, 100000008, 27, 100000002, 100000007, 1, 827, 570, 2,
                                                  582, 571, 19, 572, 100000200, 6, 100000005, 573, 3, 574, 575, 8, 581, 575, 15, 576, 577, 17, 771, 577, 33, 578,
                                                  100000128, 27, 580, 579, 68, 100000191, 100000192, 68, 100000189, 100000190, 27, 100000098, 100000099, 19, 601, 583,
                                                  20, 589, 584, 79, 100000009, 585, 80, 100000009, 586, 81, 587, 591, 10, 100000172, 588, 36, 593, 100000172, 79,
                                                  100000009, 590, 80, 100000009, 591, 10, 869, 592, 36, 593, 100000200, 27, 862, 594, 26, 595, 596, 40, 721, 596, 56,
                                                  717, 597, 41, 598, 100000171, 50, 599, 723, 51, 100000171, 600, 57, 728, 951, 6, 100000005, 602, 3, 603, 604, 8, 614,
                                                  604, 7, 605, 606, 9, 610, 606, 20, 608, 607, 79, 609, 618, 79, 609, 620, 12, 645, 100000009, 20, 612, 611, 79, 613,
                                                  672, 79, 613, 678, 12, 801, 100000009, 7, 615, 616, 9, 670, 616, 20, 619, 617, 79, 643, 618, 80, 621, 673, 79, 643,
                                                  620, 80, 621, 679, 12, 622, 100000009, 27, 633, 623, 30, 624, 625, 82, 100000078, 625, 83, 626, 627, 32, 100000082,
                                                  627, 45, 628, 629, 86, 100000086, 629, 87, 630, 631, 53, 100000090, 631, 55, 632, 100000036, 89, 100000094,
                                                  100000036, 30, 634, 635, 82, 100000077, 635, 83, 636, 637, 32, 100000081, 637, 45, 638, 639, 86, 100000085, 639, 87,
                                                  640, 641, 53, 100000089, 641, 55, 642, 100000036, 89, 100000093, 100000036, 12, 644, 799, 37, 826, 645, 27, 658, 646,
                                                  82, 647, 648, 31, 656, 648, 84, 649, 650, 44, 100000038, 650, 85, 651, 652, 46, 100000046, 652, 47, 653, 654, 86,
                                                  100000050, 654, 54, 655, 100000036, 88, 100000054, 100000036, 84, 657, 100000042, 44, 100000038, 100000042, 82, 659,
                                                  660, 31, 668, 660, 84, 661, 662, 44, 100000037, 662, 85, 663, 664, 46, 100000045, 664, 47, 665, 666, 86, 100000049,
                                                  666, 54, 667, 100000036, 88, 100000053, 100000036, 84, 669, 100000041, 44, 100000037, 100000041, 20, 677, 671, 79,
                                                  798, 672, 80, 776, 673, 81, 674, 679, 10, 897, 675, 12, 753, 676, 36, 682, 897, 79, 798, 678, 80, 776, 679, 10, 762,
                                                  680, 12, 753, 681, 36, 682, 763, 21, 683, 687, 22, 684, 687, 23, 685, 687, 16, 686, 687, 24, 1012, 687, 27, 734, 688,
                                                  26, 689, 690, 40, 721, 690, 56, 717, 691, 41, 692, 100000171, 50, 693, 723, 51, 100000171, 694, 57, 728, 695, 78,
                                                  696, 951, 58, 707, 697, 49, 698, 100000143, 52, 100000143, 699, 39, 700, 702, 42, 701, 702, 61, 702, 100000132, 62,
                                                  703, 100000140, 75, 704, 100000139, 72, 705, 100000151, 76, 706, 100000138, 73, 100000147, 100000137, 49, 708,
                                                  100000143, 52, 100000143, 709, 39, 710, 712, 42, 711, 712, 61, 712, 100000132, 62, 713, 100000140, 75, 714,
                                                  100000139, 71, 100000133, 715, 76, 716, 100000138, 73, 100000135, 100000137, 41, 718, 100000171, 50, 719, 723, 51,
                                                  100000171, 720, 57, 728, 972, 41, 722, 100000171, 50, 726, 723, 51, 100000171, 724, 58, 100000144, 725, 59,
                                                  100000160, 100000170, 51, 100000171, 727, 57, 728, 982, 58, 732, 729, 49, 730, 100000143, 52, 100000143, 731, 74,
                                                  100000153, 100000158, 49, 733, 100000143, 52, 100000143, 100000142, 26, 735, 736, 40, 974, 736, 56, 967, 737, 41,
                                                  738, 100000171, 50, 739, 976, 51, 100000171, 740, 57, 984, 741, 78, 742, 950, 58, 988, 743, 49, 744, 100000143, 52,
                                                  100000143, 745, 39, 746, 748, 42, 747, 748, 61, 748, 100000132, 62, 749, 100000140, 75, 750, 100000139, 72, 751,
                                                  100000151, 76, 752, 100000138, 73, 100000146, 100000137, 21, 754, 758, 22, 755, 758, 23, 756, 758, 16, 757, 758, 24,
                                                  1012, 758, 34, 759, 100000128, 27, 761, 760, 68, 100000113, 100000114, 68, 100000111, 100000112, 11, 1007, 763, 21,
                                                  764, 768, 22, 765, 768, 23, 766, 768, 16, 767, 768, 24, 1012, 768, 37, 773, 769, 15, 770, 100000200, 17, 771,
                                                  100000200, 33, 772, 100000128, 27, 100000129, 100000187, 27, 775, 774, 68, 100000176, 100000177, 68, 100000174,
                                                  100000175, 12, 777, 100000009, 27, 788, 778, 30, 779, 780, 82, 100000058, 780, 83, 781, 782, 32, 100000062, 782, 45,
                                                  783, 784, 86, 100000066, 784, 87, 785, 786, 53, 100000070, 786, 55, 787, 100000036, 89, 100000074, 100000036, 30,
                                                  789, 790, 82, 100000057, 790, 83, 791, 792, 32, 100000061, 792, 45, 793, 794, 86, 100000065, 794, 87, 795, 796, 53,
                                                  100000069, 796, 55, 797, 100000036, 89, 100000073, 100000036, 12, 800, 799, 37, 826, 100000009, 37, 826, 801, 27,
                                                  814, 802, 82, 803, 804, 31, 812, 804, 84, 805, 806, 44, 100000017, 806, 85, 807, 808, 46, 100000025, 808, 47, 809,
                                                  810, 86, 100000029, 810, 54, 811, 100000036, 88, 100000033, 100000036, 84, 813, 100000021, 44, 100000017, 100000021,
                                                  82, 815, 816, 31, 824, 816, 84, 817, 818, 44, 100000016, 818, 85, 819, 820, 46, 100000024, 820, 47, 821, 822, 86,
                                                  100000028, 822, 54, 823, 100000036, 88, 100000032, 100000036, 84, 825, 100000020, 44, 100000016, 100000020, 27,
                                                  100000012, 100000013, 27, 100000002, 100000003, 5, 100000001, 829, 1, 1019, 830, 2, 843, 831, 19, 832, 100000200, 3,
                                                  833, 834, 8, 842, 834, 15, 835, 836, 17, 1005, 836, 33, 837, 100000128, 27, 841, 838, 68, 100000197, 839, 69, 840,
                                                  100000199, 64, 100000198, 100000199, 68, 100000193, 100000194, 27, 100000100, 100000101, 19, 870, 844, 20, 850, 845,
                                                  79, 100000006, 846, 80, 100000006, 847, 81, 848, 852, 10, 100000172, 849, 36, 854, 100000172, 79, 100000006, 851, 80,
                                                  100000006, 852, 10, 869, 853, 36, 854, 100000200, 27, 862, 855, 26, 856, 857, 40, 928, 857, 56, 923, 858, 41, 859,
                                                  100000171, 50, 860, 930, 51, 100000171, 861, 57, 936, 921, 26, 863, 864, 40, 974, 864, 56, 967, 865, 41, 866,
                                                  100000171, 50, 867, 976, 51, 100000171, 868, 57, 984, 950, 11, 100000173, 100000200, 6, 890, 871, 20, 877, 872, 79,
                                                  100000006, 873, 80, 100000006, 874, 81, 875, 879, 10, 897, 876, 12, 881, 896, 79, 100000006, 878, 80, 100000006, 879,
                                                  10, 996, 880, 12, 881, 906, 21, 882, 886, 22, 883, 886, 23, 884, 886, 16, 885, 886, 24, 1012, 886, 34, 887,
                                                  100000128, 27, 889, 888, 68, 100000123, 100000124, 68, 100000115, 100000116, 20, 902, 891, 79, 100000006, 892, 80,
                                                  100000006, 893, 81, 894, 904, 10, 897, 895, 12, 990, 896, 36, 907, 897, 21, 898, 100000172, 22, 899, 100000172, 23,
                                                  900, 100000172, 16, 901, 100000172, 24, 1012, 100000172, 79, 100000006, 903, 80, 100000006, 904, 10, 996, 905, 12,
                                                  990, 906, 36, 907, 997, 21, 908, 912, 22, 909, 912, 23, 910, 912, 16, 911, 912, 24, 1012, 912, 27, 942, 913, 26, 914,
                                                  915, 40, 928, 915, 56, 923, 916, 41, 917, 100000171, 50, 918, 930, 51, 100000171, 919, 57, 936, 920, 78, 922, 921,
                                                  58, 940, 951, 58, 940, 957, 41, 924, 100000171, 50, 925, 930, 51, 100000171, 926, 57, 936, 927, 58, 940, 972, 41,
                                                  929, 100000171, 50, 933, 930, 51, 100000171, 931, 58, 100000144, 932, 59, 100000161, 100000170, 51, 100000171, 934,
                                                  57, 936, 935, 58, 940, 982, 58, 940, 937, 49, 938, 100000143, 52, 100000143, 939, 74, 100000154, 100000158, 49, 941,
                                                  100000143, 52, 100000143, 100000130, 26, 943, 944, 40, 974, 944, 56, 967, 945, 41, 946, 100000171, 50, 947, 976, 51,
                                                  100000171, 948, 57, 984, 949, 78, 956, 950, 58, 988, 951, 49, 952, 100000143, 52, 100000143, 953, 39, 954, 100000141,
                                                  42, 955, 100000141, 61, 100000141, 100000132, 58, 988, 957, 49, 958, 100000143, 52, 100000143, 959, 39, 960, 962, 42,
                                                  961, 962, 61, 962, 100000132, 62, 963, 100000140, 75, 964, 100000139, 72, 965, 100000151, 76, 966, 100000138, 73,
                                                  100000145, 100000137, 41, 968, 100000171, 50, 969, 976, 51, 100000171, 970, 57, 984, 971, 58, 988, 972, 49, 973,
                                                  100000143, 52, 100000143, 100000141, 41, 975, 100000171, 50, 979, 976, 51, 100000171, 977, 58, 100000144, 978, 59,
                                                  100000159, 100000170, 51, 100000171, 980, 57, 984, 981, 58, 988, 982, 49, 983, 100000143, 52, 100000143, 100000131,
                                                  58, 988, 985, 49, 986, 100000143, 52, 100000143, 987, 74, 100000152, 100000158, 49, 989, 100000143, 52, 100000143,
                                                  100000129, 21, 991, 995, 22, 992, 995, 23, 993, 995, 16, 994, 995, 24, 1012, 995, 34, 100000110, 100000128, 11, 1007,
                                                  997, 21, 998, 1002, 22, 999, 1002, 23, 1000, 1002, 16, 1001, 1002, 24, 1012, 1002, 37, 100000185, 1003, 15, 1004,
                                                  100000200, 17, 1005, 100000200, 33, 1006, 100000128, 27, 100000129, 100000130, 21, 1008, 100000173, 22, 1009,
                                                  100000173, 23, 1010, 100000173, 16, 1011, 100000173, 24, 1012, 100000173, 18, 1016, 1013, 35, 1014, 100000108, 28,
                                                  100000104, 1015, 29, 100000106, 100000107, 35, 1017, 100000108, 28, 100000102, 1018, 29, 100000102, 100000107, 27,
                                                  100000002, 100000004 };

    private static final ConditionFn[] CONDITION_FNS = { BddCostOpt2Runtime4::cond0, BddCostOpt2Runtime4::cond1,
                                                         BddCostOpt2Runtime4::cond2, BddCostOpt2Runtime4::cond3, BddCostOpt2Runtime4::cond4,
                                                         BddCostOpt2Runtime4::cond5, BddCostOpt2Runtime4::cond6, BddCostOpt2Runtime4::cond7,
                                                         BddCostOpt2Runtime4::cond8, BddCostOpt2Runtime4::cond9, BddCostOpt2Runtime4::cond10,
                                                         BddCostOpt2Runtime4::cond11, BddCostOpt2Runtime4::cond12, BddCostOpt2Runtime4::cond13,
                                                         BddCostOpt2Runtime4::cond14, BddCostOpt2Runtime4::cond15, BddCostOpt2Runtime4::cond16,
                                                         BddCostOpt2Runtime4::cond17, BddCostOpt2Runtime4::cond18, BddCostOpt2Runtime4::cond19,
                                                         BddCostOpt2Runtime4::cond20, BddCostOpt2Runtime4::cond21, BddCostOpt2Runtime4::cond22,
                                                         BddCostOpt2Runtime4::cond23, BddCostOpt2Runtime4::cond24, BddCostOpt2Runtime4::cond25,
                                                         BddCostOpt2Runtime4::cond26, BddCostOpt2Runtime4::cond27, BddCostOpt2Runtime4::cond28,
                                                         BddCostOpt2Runtime4::cond29, BddCostOpt2Runtime4::cond30, BddCostOpt2Runtime4::cond31,
                                                         BddCostOpt2Runtime4::cond32, BddCostOpt2Runtime4::cond33, BddCostOpt2Runtime4::cond34,
                                                         BddCostOpt2Runtime4::cond35, BddCostOpt2Runtime4::cond36, BddCostOpt2Runtime4::cond37,
                                                         BddCostOpt2Runtime4::cond38, BddCostOpt2Runtime4::cond39, BddCostOpt2Runtime4::cond40,
                                                         BddCostOpt2Runtime4::cond41, BddCostOpt2Runtime4::cond42, BddCostOpt2Runtime4::cond43,
                                                         BddCostOpt2Runtime4::cond44, BddCostOpt2Runtime4::cond45, BddCostOpt2Runtime4::cond46,
                                                         BddCostOpt2Runtime4::cond47, BddCostOpt2Runtime4::cond48, BddCostOpt2Runtime4::cond49,
                                                         BddCostOpt2Runtime4::cond50, BddCostOpt2Runtime4::cond51, BddCostOpt2Runtime4::cond52,
                                                         BddCostOpt2Runtime4::cond53, BddCostOpt2Runtime4::cond54, BddCostOpt2Runtime4::cond55,
                                                         BddCostOpt2Runtime4::cond56, BddCostOpt2Runtime4::cond57, BddCostOpt2Runtime4::cond58,
                                                         BddCostOpt2Runtime4::cond59, BddCostOpt2Runtime4::cond60, BddCostOpt2Runtime4::cond61,
                                                         BddCostOpt2Runtime4::cond62, BddCostOpt2Runtime4::cond63, BddCostOpt2Runtime4::cond64,
                                                         BddCostOpt2Runtime4::cond65, BddCostOpt2Runtime4::cond66, BddCostOpt2Runtime4::cond67,
                                                         BddCostOpt2Runtime4::cond68, BddCostOpt2Runtime4::cond69, BddCostOpt2Runtime4::cond70,
                                                         BddCostOpt2Runtime4::cond71, BddCostOpt2Runtime4::cond72, BddCostOpt2Runtime4::cond73,
                                                         BddCostOpt2Runtime4::cond74, BddCostOpt2Runtime4::cond75, BddCostOpt2Runtime4::cond76,
                                                         BddCostOpt2Runtime4::cond77, BddCostOpt2Runtime4::cond78, BddCostOpt2Runtime4::cond79,
                                                         BddCostOpt2Runtime4::cond80, BddCostOpt2Runtime4::cond81, BddCostOpt2Runtime4::cond82,
                                                         BddCostOpt2Runtime4::cond83, BddCostOpt2Runtime4::cond84, BddCostOpt2Runtime4::cond85,
                                                         BddCostOpt2Runtime4::cond86, BddCostOpt2Runtime4::cond87, BddCostOpt2Runtime4::cond88,
                                                         BddCostOpt2Runtime4::cond89, BddCostOpt2Runtime4::cond90

    };

    private static final ResultFn[] RESULT_FNS = { BddCostOpt2Runtime4::result0, BddCostOpt2Runtime4::result1,
                                                   BddCostOpt2Runtime4::result2, BddCostOpt2Runtime4::result3, BddCostOpt2Runtime4::result4,
                                                   BddCostOpt2Runtime4::result5, BddCostOpt2Runtime4::result6, BddCostOpt2Runtime4::result7,
                                                   BddCostOpt2Runtime4::result8, BddCostOpt2Runtime4::result9, BddCostOpt2Runtime4::result10,
                                                   BddCostOpt2Runtime4::result11, BddCostOpt2Runtime4::result12, BddCostOpt2Runtime4::result13,
                                                   BddCostOpt2Runtime4::result14, BddCostOpt2Runtime4::result15, BddCostOpt2Runtime4::result16,
                                                   BddCostOpt2Runtime4::result17, BddCostOpt2Runtime4::result18, BddCostOpt2Runtime4::result19,
                                                   BddCostOpt2Runtime4::result20, BddCostOpt2Runtime4::result21, BddCostOpt2Runtime4::result22,
                                                   BddCostOpt2Runtime4::result23, BddCostOpt2Runtime4::result24, BddCostOpt2Runtime4::result25,
                                                   BddCostOpt2Runtime4::result26, BddCostOpt2Runtime4::result27, BddCostOpt2Runtime4::result28,
                                                   BddCostOpt2Runtime4::result29, BddCostOpt2Runtime4::result30, BddCostOpt2Runtime4::result31,
                                                   BddCostOpt2Runtime4::result32, BddCostOpt2Runtime4::result33, BddCostOpt2Runtime4::result34,
                                                   BddCostOpt2Runtime4::result35, BddCostOpt2Runtime4::result36, BddCostOpt2Runtime4::result37,
                                                   BddCostOpt2Runtime4::result38, BddCostOpt2Runtime4::result39, BddCostOpt2Runtime4::result40,
                                                   BddCostOpt2Runtime4::result41, BddCostOpt2Runtime4::result42, BddCostOpt2Runtime4::result43,
                                                   BddCostOpt2Runtime4::result44, BddCostOpt2Runtime4::result45, BddCostOpt2Runtime4::result46,
                                                   BddCostOpt2Runtime4::result47, BddCostOpt2Runtime4::result48, BddCostOpt2Runtime4::result49,
                                                   BddCostOpt2Runtime4::result50, BddCostOpt2Runtime4::result51, BddCostOpt2Runtime4::result52,
                                                   BddCostOpt2Runtime4::result53, BddCostOpt2Runtime4::result54, BddCostOpt2Runtime4::result55,
                                                   BddCostOpt2Runtime4::result56, BddCostOpt2Runtime4::result57, BddCostOpt2Runtime4::result58,
                                                   BddCostOpt2Runtime4::result59, BddCostOpt2Runtime4::result60, BddCostOpt2Runtime4::result61,
                                                   BddCostOpt2Runtime4::result62, BddCostOpt2Runtime4::result63, BddCostOpt2Runtime4::result64,
                                                   BddCostOpt2Runtime4::result65, BddCostOpt2Runtime4::result66, BddCostOpt2Runtime4::result67,
                                                   BddCostOpt2Runtime4::result68, BddCostOpt2Runtime4::result69, BddCostOpt2Runtime4::result70,
                                                   BddCostOpt2Runtime4::result71, BddCostOpt2Runtime4::result72, BddCostOpt2Runtime4::result73,
                                                   BddCostOpt2Runtime4::result74, BddCostOpt2Runtime4::result75, BddCostOpt2Runtime4::result76,
                                                   BddCostOpt2Runtime4::result77, BddCostOpt2Runtime4::result78, BddCostOpt2Runtime4::result79,
                                                   BddCostOpt2Runtime4::result80, BddCostOpt2Runtime4::result81, BddCostOpt2Runtime4::result82,
                                                   BddCostOpt2Runtime4::result83, BddCostOpt2Runtime4::result84, BddCostOpt2Runtime4::result85,
                                                   BddCostOpt2Runtime4::result86, BddCostOpt2Runtime4::result87, BddCostOpt2Runtime4::result88,
                                                   BddCostOpt2Runtime4::result89, BddCostOpt2Runtime4::result90, BddCostOpt2Runtime4::result91,
                                                   BddCostOpt2Runtime4::result92, BddCostOpt2Runtime4::result93, BddCostOpt2Runtime4::result94,
                                                   BddCostOpt2Runtime4::result95, BddCostOpt2Runtime4::result96, BddCostOpt2Runtime4::result97,
                                                   BddCostOpt2Runtime4::result98, BddCostOpt2Runtime4::result99, BddCostOpt2Runtime4::result100,
                                                   BddCostOpt2Runtime4::result101, BddCostOpt2Runtime4::result102, BddCostOpt2Runtime4::result103,
                                                   BddCostOpt2Runtime4::result104, BddCostOpt2Runtime4::result105, BddCostOpt2Runtime4::result106,
                                                   BddCostOpt2Runtime4::result107, BddCostOpt2Runtime4::result108, BddCostOpt2Runtime4::result109,
                                                   BddCostOpt2Runtime4::result110, BddCostOpt2Runtime4::result111, BddCostOpt2Runtime4::result112,
                                                   BddCostOpt2Runtime4::result113, BddCostOpt2Runtime4::result114, BddCostOpt2Runtime4::result115,
                                                   BddCostOpt2Runtime4::result116, BddCostOpt2Runtime4::result117, BddCostOpt2Runtime4::result118,
                                                   BddCostOpt2Runtime4::result119, BddCostOpt2Runtime4::result120, BddCostOpt2Runtime4::result121,
                                                   BddCostOpt2Runtime4::result122, BddCostOpt2Runtime4::result123, BddCostOpt2Runtime4::result124,
                                                   BddCostOpt2Runtime4::result125, BddCostOpt2Runtime4::result126, BddCostOpt2Runtime4::result127,
                                                   BddCostOpt2Runtime4::result128, BddCostOpt2Runtime4::result129, BddCostOpt2Runtime4::result130,
                                                   BddCostOpt2Runtime4::result131, BddCostOpt2Runtime4::result132, BddCostOpt2Runtime4::result133,
                                                   BddCostOpt2Runtime4::result134, BddCostOpt2Runtime4::result135, BddCostOpt2Runtime4::result136,
                                                   BddCostOpt2Runtime4::result137, BddCostOpt2Runtime4::result138, BddCostOpt2Runtime4::result139,
                                                   BddCostOpt2Runtime4::result140, BddCostOpt2Runtime4::result141, BddCostOpt2Runtime4::result142,
                                                   BddCostOpt2Runtime4::result143, BddCostOpt2Runtime4::result144, BddCostOpt2Runtime4::result145,
                                                   BddCostOpt2Runtime4::result146, BddCostOpt2Runtime4::result147, BddCostOpt2Runtime4::result148,
                                                   BddCostOpt2Runtime4::result149, BddCostOpt2Runtime4::result150, BddCostOpt2Runtime4::result151,
                                                   BddCostOpt2Runtime4::result152, BddCostOpt2Runtime4::result153, BddCostOpt2Runtime4::result154,
                                                   BddCostOpt2Runtime4::result155, BddCostOpt2Runtime4::result156, BddCostOpt2Runtime4::result157,
                                                   BddCostOpt2Runtime4::result158, BddCostOpt2Runtime4::result159, BddCostOpt2Runtime4::result160,
                                                   BddCostOpt2Runtime4::result161, BddCostOpt2Runtime4::result162, BddCostOpt2Runtime4::result163,
                                                   BddCostOpt2Runtime4::result164, BddCostOpt2Runtime4::result165, BddCostOpt2Runtime4::result166,
                                                   BddCostOpt2Runtime4::result167, BddCostOpt2Runtime4::result168, BddCostOpt2Runtime4::result169,
                                                   BddCostOpt2Runtime4::result170, BddCostOpt2Runtime4::result171, BddCostOpt2Runtime4::result172,
                                                   BddCostOpt2Runtime4::result173, BddCostOpt2Runtime4::result174, BddCostOpt2Runtime4::result175,
                                                   BddCostOpt2Runtime4::result176, BddCostOpt2Runtime4::result177, BddCostOpt2Runtime4::result178,
                                                   BddCostOpt2Runtime4::result179, BddCostOpt2Runtime4::result180, BddCostOpt2Runtime4::result181,
                                                   BddCostOpt2Runtime4::result182, BddCostOpt2Runtime4::result183, BddCostOpt2Runtime4::result184,
                                                   BddCostOpt2Runtime4::result185, BddCostOpt2Runtime4::result186, BddCostOpt2Runtime4::result187,
                                                   BddCostOpt2Runtime4::result188, BddCostOpt2Runtime4::result189, BddCostOpt2Runtime4::result190,
                                                   BddCostOpt2Runtime4::result191, BddCostOpt2Runtime4::result192, BddCostOpt2Runtime4::result193,
                                                   BddCostOpt2Runtime4::result194, BddCostOpt2Runtime4::result195, BddCostOpt2Runtime4::result196,
                                                   BddCostOpt2Runtime4::result197, BddCostOpt2Runtime4::result198, BddCostOpt2Runtime4::result199

    };

    private static boolean cond0(Registers registers) {
        return (registers.region != null);
    }

    private static boolean cond1(Registers registers) {
        return (registers.endpoint != null);
    }

    private static boolean cond2(Registers registers) {
        return (registers.bucket != null);
    }

    private static boolean cond3(Registers registers) {
        return (registers.useS3ExpressControlEndpoint != null);
    }

    private static boolean cond4(Registers registers) {
        return (registers.accelerate);
    }

    private static boolean cond5(Registers registers) {
        return (registers.useFIPS);
    }

    private static boolean cond6(Registers registers) {
        return ("aws-cn".equals(registers.partitionResult.name()));
    }

    private static boolean cond7(Registers registers) {
        return (registers.disableS3ExpressSessionAuth != null);
    }

    private static boolean cond8(Registers registers) {
        return (Boolean.FALSE != registers.useS3ExpressControlEndpoint);
    }

    private static boolean cond9(Registers registers) {
        return (Boolean.FALSE != registers.disableS3ExpressSessionAuth);
    }

    private static boolean cond10(Registers registers) {
        return (registers.forcePathStyle);
    }

    private static boolean cond11(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond12(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, false));
    }

    private static boolean cond13(Registers registers) {
        return (RulesFunctions.awsIsVirtualHostableS3Bucket(registers.bucket, true));
    }

    private static boolean cond14(Registers registers) {
        return ("http".equals(registers.url.scheme()));
    }

    private static boolean cond15(Registers registers) {
        return (registers.useObjectLambdaEndpoint != null);
    }

    private static boolean cond16(Registers registers) {
        return ("--op-s3".equals(registers.accessPointSuffix));
    }

    private static boolean cond17(Registers registers) {
        return (Boolean.FALSE != registers.useObjectLambdaEndpoint);
    }

    private static boolean cond18(Registers registers) {
        return ("beta".equals(registers.regionPrefix));
    }

    private static boolean cond19(Registers registers) {
        registers.partitionResult = RulesFunctions.awsPartition(registers.region);
        return registers.partitionResult != null;
    }

    private static boolean cond20(Registers registers) {
        return (RulesFunctions.awsParseArn(registers.bucket) != null);
    }

    private static boolean cond21(Registers registers) {
        registers.outpostId_ssa_2 = RulesFunctions.substring(registers.bucket, 32, 49, true);
        return registers.outpostId_ssa_2 != null;
    }

    private static boolean cond22(Registers registers) {
        registers.hardwareType = RulesFunctions.substring(registers.bucket, 49, 50, true);
        return registers.hardwareType != null;
    }

    private static boolean cond23(Registers registers) {
        registers.accessPointSuffix = RulesFunctions.substring(registers.bucket, 0, 7, true);
        return registers.accessPointSuffix != null;
    }

    private static boolean cond24(Registers registers) {
        registers.regionPrefix = RulesFunctions.substring(registers.bucket, 8, 12, true);
        return registers.regionPrefix != null;
    }

    private static boolean cond25(Registers registers) {
        return (registers.url.isIp());
    }

    private static boolean cond26(Registers registers) {
        return (registers.disableAccessPoints != null);
    }

    private static boolean cond27(Registers registers) {
        return (registers.useDualStack);
    }

    private static boolean cond28(Registers registers) {
        return ("e".equals(registers.hardwareType));
    }

    private static boolean cond29(Registers registers) {
        return ("o".equals(registers.hardwareType));
    }

    private static boolean cond30(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_6 = RulesFunctions.substring(registers.bucket, 7, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_6 != null;
    }

    private static boolean cond31(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_2 = RulesFunctions.substring(registers.bucket, 6, 15, true);
        return registers.s3expressAvailabilityZoneId_ssa_2 != null;
    }

    private static boolean cond32(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_7 = RulesFunctions.substring(registers.bucket, 7, 16, true);
        return registers.s3expressAvailabilityZoneId_ssa_7 != null;
    }

    private static boolean cond33(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, true));
    }

    private static boolean cond34(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.region, false));
    }

    private static boolean cond35(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_2, false));
    }

    private static boolean cond36(Registers registers) {
        registers.bucketArn = RulesFunctions.awsParseArn(registers.bucket);
        return registers.bucketArn != null;
    }

    private static boolean cond37(Registers registers) {
        registers.uri_encoded_bucket = RulesFunctions.uriEncode(registers.bucket);
        return registers.uri_encoded_bucket != null;
    }

    private static boolean cond38(Registers registers) {
        registers.url = RulesFunctions.parseURL(registers.endpoint);
        return registers.url != null;
    }

    private static boolean cond39(Registers registers) {
        return (registers.useArnRegion != null);
    }

    private static boolean cond40(Registers registers) {
        return (Boolean.FALSE != registers.disableAccessPoints);
    }

    private static boolean cond41(Registers registers) {
        registers.arnType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 0);
        return registers.arnType != null;
    }

    private static boolean cond42(Registers registers) {
        return (!registers.useArnRegion);
    }

    private static boolean cond43(Registers registers) {
        registers.outpostId_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.outpostId_ssa_1 != null;
    }

    private static boolean cond44(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_1 = RulesFunctions.substring(registers.bucket, 6, 14, true);
        return registers.s3expressAvailabilityZoneId_ssa_1 != null;
    }

    private static boolean cond45(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_8 = RulesFunctions.substring(registers.bucket, 7, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_8 != null;
    }

    private static boolean cond46(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_3 = RulesFunctions.substring(registers.bucket, 6, 19, true);
        return registers.s3expressAvailabilityZoneId_ssa_3 != null;
    }

    private static boolean cond47(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_4 = RulesFunctions.substring(registers.bucket, 6, 20, true);
        return registers.s3expressAvailabilityZoneId_ssa_4 != null;
    }

    private static boolean cond48(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 4) != null);
    }

    private static boolean cond49(Registers registers) {
        registers.accessPointName_ssa_1 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 1);
        return registers.accessPointName_ssa_1 != null;
    }

    private static boolean cond50(Registers registers) {
        return ("accesspoint".equals(registers.arnType));
    }

    private static boolean cond51(Registers registers) {
        return ("".equals(registers.arnType));
    }

    private static boolean cond52(Registers registers) {
        return ("".equals(registers.accessPointName_ssa_1));
    }

    private static boolean cond53(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_9 = RulesFunctions.substring(registers.bucket, 7, 21, true);
        return registers.s3expressAvailabilityZoneId_ssa_9 != null;
    }

    private static boolean cond54(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_5 = RulesFunctions.substring(registers.bucket, 6, 26, true);
        return registers.s3expressAvailabilityZoneId_ssa_5 != null;
    }

    private static boolean cond55(Registers registers) {
        registers.s3expressAvailabilityZoneId_ssa_10 = RulesFunctions.substring(registers.bucket, 7, 27, true);
        return registers.s3expressAvailabilityZoneId_ssa_10 != null;
    }

    private static boolean cond56(Registers registers) {
        return (RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2) != null);
    }

    private static boolean cond57(Registers registers) {
        return ("".equals(registers.bucketArn.region()));
    }

    private static boolean cond58(Registers registers) {
        return ("s3-object-lambda".equals(registers.bucketArn.service()));
    }

    private static boolean cond59(Registers registers) {
        return ("s3-outposts".equals(registers.bucketArn.service()));
    }

    private static boolean cond60(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.outpostId_ssa_1, false));
    }

    private static boolean cond61(Registers registers) {
        return (RulesFunctions.stringEquals(registers.region, registers.bucketArn.region()));
    }

    private static boolean cond62(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketPartition.name(), registers.partitionResult.name()));
    }

    private static boolean cond63(Registers registers) {
        return (registers.disableMultiRegionAccessPoints);
    }

    private static boolean cond64(Registers registers) {
        return (registers.useGlobalEndpoint);
    }

    private static boolean cond65(Registers registers) {
        registers.outpostType = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 2);
        return registers.outpostType != null;
    }

    private static boolean cond66(Registers registers) {
        registers.accessPointName_ssa_2 = RulesFunctions.listAccess(registers.bucketArn.resourceId(), 3);
        return registers.accessPointName_ssa_2 != null;
    }

    private static boolean cond67(Registers registers) {
        return ("accesspoint".equals(registers.outpostType));
    }

    private static boolean cond68(Registers registers) {
        return ("aws-global".equals(registers.region));
    }

    private static boolean cond69(Registers registers) {
        return ("us-east-1".equals(registers.region));
    }

    private static boolean cond70(Registers registers) {
        return (!registers.url.isIp());
    }

    private static boolean cond71(Registers registers) {
        return ("".equals(registers.bucketArn.accountId()));
    }

    private static boolean cond72(Registers registers) {
        return ("s3".equals(registers.bucketArn.service()));
    }

    private static boolean cond73(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, false));
    }

    private static boolean cond74(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.accessPointName_ssa_1, true));
    }

    private static boolean cond75(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.region(), true));
    }

    private static boolean cond76(Registers registers) {
        return (RulesFunctions.isValidHostLabel(registers.bucketArn.accountId(), false));
    }

    private static boolean cond77(Registers registers) {
        return (RulesFunctions.stringEquals(registers.bucketArn.partition(), registers.partitionResult.name()));
    }

    private static boolean cond78(Registers registers) {
        registers.bucketPartition = RulesFunctions.awsPartition(registers.bucketArn.region());
        return registers.bucketPartition != null;
    }

    private static boolean cond79(Registers registers) {
        return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 6, true), "")));
    }

    private static boolean cond80(Registers registers) {
        return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 7, true), "")));
    }

    private static boolean cond81(Registers registers) {
        return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 0, 4, false), "")));
    }

    private static boolean cond82(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 15, 17, true), "")));
    }

    private static boolean cond83(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 16, 18, true), "")));
    }

    private static boolean cond84(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 14, 16, true), "")));
    }

    private static boolean cond85(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 19, 21, true), "")));
    }

    private static boolean cond86(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 20, 22, true), "")));
    }

    private static boolean cond87(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 21, 23, true), "")));
    }

    private static boolean cond88(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 26, 28, true), "")));
    }

    private static boolean cond89(Registers registers) {
        return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(registers.bucket, 27, 29, true), "")));
    }

    private static boolean cond90(Registers registers) {
        return (RulesFunctions.parseURL(registers.endpoint) != null);
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
            System.out.println("Node " + (nodeRef-1) + ", C" + bdd[base] + " => " + conditionResult);
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

        String arnType;

        String outpostId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_1;

        String s3expressAvailabilityZoneId_ssa_8;

        String s3expressAvailabilityZoneId_ssa_3;

        String s3expressAvailabilityZoneId_ssa_4;

        String accessPointName_ssa_1;

        String s3expressAvailabilityZoneId_ssa_9;

        String s3expressAvailabilityZoneId_ssa_5;

        String s3expressAvailabilityZoneId_ssa_10;

        String outpostType;

        String accessPointName_ssa_2;

        RulePartition bucketPartition;
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
