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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
// optimized using split + ite(ternary) + new express result templates
// replaces Registery with Evaluator class with cond/result switches.
// condition and result functions inlined
public final class BddCostOpt4Runtime6a implements S3EndpointProvider {
    private static final int[] BDD_DEFINITION = { -1, 1, -1, 0, 3, 100000113, 1, 61, 4, 4, 5, 7, 5, 6, 7, 8, 17, 7, 18, 8, 20,
                                                  19, 708, 9, 20, 707, 10, 24, 11, 589, 28, 12, 47, 36, 13, 14, 37, 590, 14, 38, 15, 591, 39, 100000002, 16, 66,
                                                  100000108, 100000109, 15, 18, 19, 17, 49, 19, 18, 43, 20, 19, 34, 21, 20, 25, 22, 24, 23, 100000113, 36, 24, 28, 37,
                                                  174, 28, 24, 26, 100000113, 36, 27, 28, 37, 256, 28, 38, 29, 100000041, 39, 33, 30, 66, 100000110, 31, 69, 32,
                                                  100000112, 74, 100000111, 100000112, 66, 100000106, 100000107, 20, 100000001, 35, 24, 36, 100000113, 25, 100000005,
                                                  37, 36, 38, 39, 37, 379, 39, 38, 40, 100000041, 39, 42, 41, 66, 100000104, 100000105, 66, 100000102, 100000103, 19,
                                                  708, 44, 20, 707, 45, 24, 46, 589, 28, 60, 47, 36, 48, 591, 37, 583, 591, 18, 55, 50, 19, 52, 51, 24, 100000014,
                                                  100000113, 20, 100000001, 53, 24, 54, 100000113, 25, 100000005, 100000014, 19, 708, 56, 20, 707, 57, 24, 58, 589, 28,
                                                  60, 59, 39, 100000002, 100000014, 39, 100000002, 100000013, 2, 126, 62, 3, 639, 63, 6, 619, 64, 10, 65, 70, 11, 66,
                                                  70, 12, 67, 70, 13, 68, 70, 14, 69, 70, 16, 596, 70, 18, 106, 71, 19, 91, 72, 20, 81, 73, 22, 78, 74, 24, 76, 75, 31,
                                                  140, 100, 27, 163, 77, 31, 143, 80, 23, 105, 79, 24, 80, 100, 32, 100000085, 171, 22, 88, 82, 24, 84, 83, 31, 184,
                                                  100, 25, 86, 85, 27, 197, 87, 27, 250, 87, 31, 203, 90, 23, 105, 89, 24, 90, 100, 32, 100000085, 253, 20, 100000001,
                                                  92, 22, 98, 93, 24, 95, 94, 31, 262, 100, 25, 100000005, 96, 27, 369, 97, 31, 286, 102, 23, 103, 99, 24, 101, 100,
                                                  32, 100000085, 100000113, 25, 100000005, 102, 32, 100000085, 376, 24, 104, 105, 25, 100000005, 105, 32, 100000085,
                                                  100000086, 19, 708, 107, 20, 707, 108, 21, 109, 389, 22, 118, 110, 24, 112, 111, 31, 393, 120, 27, 569, 113, 28, 115,
                                                  114, 31, 423, 122, 29, 116, 117, 30, 567, 117, 31, 482, 123, 23, 124, 119, 24, 121, 120, 32, 125, 589, 28, 123, 122,
                                                  32, 125, 580, 32, 125, 586, 32, 125, 595, 39, 100000002, 100000085, 3, 639, 127, 6, 619, 128, 10, 129, 134, 11, 130,
                                                  134, 12, 131, 134, 13, 132, 134, 14, 133, 134, 16, 596, 134, 18, 386, 135, 19, 258, 136, 20, 181, 137, 22, 169, 138,
                                                  24, 141, 139, 31, 140, 100000113, 39, 273, 394, 27, 163, 142, 31, 143, 171, 39, 144, 424, 42, 145, 146, 43, 359, 146,
                                                  45, 352, 147, 51, 148, 100000084, 52, 100000084, 149, 53, 150, 362, 54, 151, 100000056, 55, 100000056, 152, 56,
                                                  100000042, 153, 59, 368, 154, 60, 155, 512, 61, 156, 158, 62, 158, 157, 70, 100000045, 158, 72, 159, 100000053, 73,
                                                  160, 100000052, 75, 161, 100000064, 79, 162, 100000051, 80, 100000061, 100000050, 33, 164, 100000041, 39, 168, 165,
                                                  66, 100000038, 166, 69, 167, 100000040, 74, 100000039, 100000040, 66, 100000030, 100000031, 23, 100000086, 170, 24,
                                                  171, 100000113, 35, 176, 172, 36, 173, 100000113, 37, 174, 100000113, 38, 175, 100000041, 39, 100000042, 100000101,
                                                  39, 180, 177, 66, 100000095, 178, 69, 179, 100000097, 74, 100000096, 100000097, 66, 100000091, 100000092, 22, 251,
                                                  182, 24, 195, 183, 31, 184, 100000113, 39, 273, 185, 42, 186, 187, 43, 221, 187, 45, 214, 188, 51, 189, 100000084,
                                                  52, 100000084, 190, 53, 191, 224, 54, 192, 100000056, 55, 100000056, 193, 56, 100000043, 194, 59, 230, 512, 25, 201,
                                                  196, 27, 197, 202, 33, 198, 100000041, 39, 200, 199, 66, 100000036, 100000037, 66, 100000028, 100000029, 27, 250,
                                                  202, 31, 203, 253, 39, 231, 204, 42, 205, 206, 43, 221, 206, 45, 214, 207, 51, 208, 100000084, 52, 100000084, 209,
                                                  53, 210, 224, 54, 211, 100000056, 55, 100000056, 212, 56, 100000043, 213, 59, 230, 241, 51, 215, 100000084, 52,
                                                  100000084, 216, 53, 217, 224, 54, 218, 100000056, 55, 100000056, 219, 56, 100000043, 220, 59, 230, 100000054, 51,
                                                  222, 100000084, 52, 100000084, 223, 53, 226, 224, 56, 100000057, 225, 57, 100000074, 100000083, 54, 227, 100000056,
                                                  55, 100000056, 228, 56, 100000043, 229, 59, 230, 100000044, 63, 100000067, 100000071, 42, 232, 233, 43, 359, 233, 45,
                                                  352, 234, 51, 235, 100000084, 52, 100000084, 236, 53, 237, 362, 54, 238, 100000056, 55, 100000056, 239, 56,
                                                  100000042, 240, 59, 368, 241, 60, 242, 512, 61, 243, 245, 62, 245, 244, 70, 100000045, 245, 72, 246, 100000053, 73,
                                                  247, 100000052, 75, 248, 100000064, 79, 249, 100000051, 80, 100000058, 100000050, 33, 100000023, 100000041, 23,
                                                  100000086, 252, 24, 253, 100000113, 35, 100000098, 254, 36, 255, 100000113, 37, 256, 100000113, 38, 257, 100000041,
                                                  39, 100000042, 100000043, 20, 100000001, 259, 22, 373, 260, 24, 283, 261, 31, 262, 100000113, 39, 273, 263, 42, 264,
                                                  265, 43, 323, 265, 45, 316, 266, 51, 267, 100000084, 52, 100000084, 268, 53, 269, 326, 54, 270, 100000056, 55,
                                                  100000056, 271, 56, 412, 272, 59, 332, 512, 42, 274, 275, 43, 359, 275, 45, 352, 276, 51, 277, 100000084, 52,
                                                  100000084, 278, 53, 279, 362, 54, 280, 100000056, 55, 100000056, 281, 56, 100000042, 282, 59, 368, 512, 25,
                                                  100000005, 284, 27, 369, 285, 31, 286, 376, 39, 333, 287, 42, 288, 289, 43, 323, 289, 45, 316, 290, 51, 291,
                                                  100000084, 52, 100000084, 292, 53, 293, 326, 54, 294, 100000056, 55, 100000056, 295, 56, 306, 296, 59, 332, 297, 60,
                                                  298, 512, 61, 299, 301, 62, 301, 300, 70, 100000045, 301, 72, 302, 100000053, 73, 303, 100000052, 75, 304, 100000064,
                                                  79, 305, 100000051, 80, 100000060, 100000050, 59, 100000055, 307, 60, 308, 512, 61, 309, 311, 62, 311, 310, 70,
                                                  100000045, 311, 72, 312, 100000053, 73, 313, 100000052, 77, 100000046, 314, 79, 315, 100000051, 80, 100000048,
                                                  100000050, 51, 317, 100000084, 52, 100000084, 318, 53, 319, 326, 54, 320, 100000056, 55, 100000056, 321, 56, 530,
                                                  322, 59, 332, 100000054, 51, 324, 100000084, 52, 100000084, 325, 53, 328, 326, 56, 100000057, 327, 57, 100000073,
                                                  100000083, 54, 329, 100000056, 55, 100000056, 330, 56, 566, 331, 59, 332, 100000044, 63, 100000066, 100000071, 42,
                                                  334, 335, 43, 359, 335, 45, 352, 336, 51, 337, 100000084, 52, 100000084, 338, 53, 339, 362, 54, 340, 100000056, 55,
                                                  100000056, 341, 56, 100000042, 342, 59, 368, 343, 60, 344, 512, 61, 345, 347, 62, 347, 346, 70, 100000045, 347, 72,
                                                  348, 100000053, 73, 349, 100000052, 75, 350, 100000064, 79, 351, 100000051, 80, 100000059, 100000050, 51, 353,
                                                  100000084, 52, 100000084, 354, 53, 355, 362, 54, 356, 100000056, 55, 100000056, 357, 56, 100000042, 358, 59, 368,
                                                  100000054, 51, 360, 100000084, 52, 100000084, 361, 53, 364, 362, 56, 100000057, 363, 57, 100000072, 100000083, 54,
                                                  365, 100000056, 55, 100000056, 366, 56, 100000042, 367, 59, 368, 100000044, 63, 100000065, 100000071, 33, 370,
                                                  100000041, 39, 372, 371, 66, 100000026, 100000027, 66, 100000024, 100000025, 23, 384, 374, 24, 375, 100000113, 25,
                                                  100000005, 376, 35, 381, 377, 36, 378, 100000113, 37, 379, 100000113, 38, 380, 100000041, 39, 100000042, 100000100,
                                                  39, 383, 382, 66, 100000089, 100000090, 66, 100000087, 100000088, 24, 385, 100000086, 25, 100000005, 100000086, 19,
                                                  708, 387, 20, 707, 388, 21, 390, 389, 39, 100000002, 100000022, 22, 577, 391, 24, 420, 392, 31, 393, 589, 39,
                                                  100000002, 394, 42, 395, 396, 43, 413, 396, 44, 404, 397, 45, 401, 398, 51, 399, 100000084, 52, 100000084, 400, 53,
                                                  408, 417, 51, 402, 100000084, 52, 100000084, 403, 53, 526, 417, 45, 523, 405, 51, 406, 100000084, 52, 100000084, 407,
                                                  53, 408, 556, 54, 409, 100000056, 55, 100000056, 410, 56, 412, 411, 59, 562, 512, 59, 100000055, 512, 44, 553, 414,
                                                  51, 415, 100000084, 52, 100000084, 416, 53, 558, 417, 56, 100000057, 418, 57, 419, 100000083, 58, 539, 100000082, 27,
                                                  569, 421, 28, 479, 422, 31, 423, 580, 39, 100000002, 424, 42, 425, 426, 43, 461, 426, 44, 434, 427, 45, 431, 428, 51,
                                                  429, 100000084, 52, 100000084, 430, 53, 438, 465, 51, 432, 100000084, 52, 100000084, 433, 53, 526, 465, 45, 523, 435,
                                                  51, 436, 100000084, 52, 100000084, 437, 53, 438, 556, 54, 439, 100000056, 55, 100000056, 440, 56, 451, 441, 59, 562,
                                                  442, 60, 443, 512, 61, 444, 446, 62, 446, 445, 70, 100000045, 446, 72, 447, 100000053, 73, 448, 100000052, 75, 449,
                                                  100000064, 79, 450, 100000051, 80, 100000063, 100000050, 59, 100000055, 452, 60, 453, 512, 61, 454, 456, 62, 456,
                                                  455, 70, 100000045, 456, 72, 457, 100000053, 73, 458, 100000052, 77, 100000046, 459, 79, 460, 100000051, 80,
                                                  100000049, 100000050, 44, 553, 462, 51, 463, 100000084, 52, 100000084, 464, 53, 558, 465, 56, 100000057, 466, 57,
                                                  467, 100000083, 58, 468, 100000082, 60, 469, 539, 61, 470, 472, 62, 472, 471, 70, 545, 472, 71, 473, 100000081, 72,
                                                  474, 100000053, 73, 475, 100000052, 79, 476, 100000051, 81, 477, 100000080, 82, 478, 100000079, 84, 100000077,
                                                  100000078, 29, 480, 481, 30, 567, 481, 31, 482, 586, 39, 100000002, 483, 42, 484, 485, 43, 531, 485, 44, 493, 486,
                                                  45, 490, 487, 51, 488, 100000084, 52, 100000084, 489, 53, 497, 535, 51, 491, 100000084, 52, 100000084, 492, 53, 526,
                                                  535, 45, 523, 494, 51, 495, 100000084, 52, 100000084, 496, 53, 497, 556, 54, 498, 100000056, 55, 100000056, 499, 56,
                                                  510, 500, 59, 562, 501, 60, 502, 512, 61, 503, 505, 62, 505, 504, 70, 100000045, 505, 72, 506, 100000053, 73, 507,
                                                  100000052, 75, 508, 100000064, 79, 509, 100000051, 80, 100000062, 100000050, 59, 100000055, 511, 60, 515, 512, 61,
                                                  513, 100000054, 62, 100000054, 514, 70, 100000045, 100000054, 61, 516, 518, 62, 518, 517, 70, 100000045, 518, 72,
                                                  519, 100000053, 73, 520, 100000052, 77, 100000046, 521, 79, 522, 100000051, 80, 100000047, 100000050, 51, 524,
                                                  100000084, 52, 100000084, 525, 53, 526, 556, 54, 527, 100000056, 55, 100000056, 528, 56, 530, 529, 59, 562,
                                                  100000054, 59, 100000055, 100000054, 44, 553, 532, 51, 533, 100000084, 52, 100000084, 534, 53, 558, 535, 56,
                                                  100000057, 536, 57, 537, 100000083, 58, 538, 100000082, 60, 542, 539, 61, 540, 100000081, 62, 100000081, 541, 70,
                                                  545, 100000081, 61, 543, 546, 62, 546, 544, 70, 545, 546, 71, 100000045, 100000081, 71, 547, 100000081, 72, 548,
                                                  100000053, 73, 549, 100000052, 79, 550, 100000051, 81, 551, 100000080, 82, 552, 100000079, 84, 100000076, 100000078,
                                                  51, 554, 100000084, 52, 100000084, 555, 53, 558, 556, 56, 100000057, 557, 57, 100000075, 100000083, 54, 559,
                                                  100000056, 55, 100000056, 560, 56, 566, 561, 59, 562, 100000044, 63, 563, 100000071, 78, 100000068, 564, 83, 565,
                                                  100000071, 85, 100000069, 100000070, 59, 100000055, 100000044, 33, 568, 591, 39, 100000002, 100000035, 28, 570, 591,
                                                  33, 571, 591, 34, 575, 572, 39, 100000002, 573, 64, 574, 100000041, 66, 100000033, 100000035, 39, 100000002, 576, 66,
                                                  100000032, 100000034, 23, 595, 578, 24, 579, 589, 28, 586, 580, 35, 585, 581, 36, 582, 589, 37, 583, 589, 38, 584,
                                                  591, 39, 100000002, 100000101, 39, 100000002, 100000098, 35, 593, 587, 36, 588, 589, 37, 590, 589, 39, 100000002,
                                                  100000113, 38, 592, 591, 39, 100000002, 100000041, 39, 100000002, 100000099, 39, 100000002, 594, 66, 100000093,
                                                  100000094, 39, 100000002, 100000086, 18, 606, 597, 19, 598, 601, 20, 100000001, 599, 24, 600, 601, 25, 100000005,
                                                  601, 26, 602, 100000021, 48, 605, 603, 49, 604, 100000020, 86, 100000015, 100000019, 86, 100000015, 100000017, 19,
                                                  708, 607, 20, 707, 608, 26, 610, 609, 39, 100000002, 100000021, 28, 614, 611, 39, 100000002, 612, 48, 100000017, 613,
                                                  49, 100000019, 100000020, 39, 100000002, 615, 48, 618, 616, 49, 617, 100000020, 86, 100000018, 100000019, 86,
                                                  100000016, 100000017, 8, 620, 653, 15, 621, 661, 17, 622, 661, 18, 630, 623, 19, 626, 624, 20, 100000006, 625, 24,
                                                  629, 100000009, 20, 100000001, 627, 24, 628, 100000009, 25, 100000005, 629, 27, 636, 100000009, 19, 708, 631, 20,
                                                  707, 632, 24, 633, 688, 27, 634, 691, 28, 703, 635, 39, 100000002, 636, 40, 702, 637, 41, 702, 638, 46, 702, 700, 4,
                                                  640, 641, 5, 652, 641, 8, 642, 653, 15, 643, 661, 17, 644, 661, 18, 685, 645, 19, 648, 646, 20, 100000006, 647, 24,
                                                  651, 100000009, 20, 100000001, 649, 24, 650, 100000009, 25, 100000005, 651, 27, 697, 100000009, 8, 659, 653, 18, 654,
                                                  662, 19, 708, 655, 20, 707, 656, 24, 657, 694, 27, 658, 694, 28, 694, 674, 15, 660, 661, 17, 675, 661, 18, 669, 662,
                                                  19, 665, 663, 20, 100000006, 664, 24, 668, 100000009, 20, 100000001, 666, 24, 667, 100000009, 25, 100000005, 668, 27,
                                                  100000012, 100000009, 19, 708, 670, 20, 707, 671, 24, 672, 688, 27, 673, 691, 28, 703, 674, 39, 100000002, 100000012,
                                                  18, 685, 676, 19, 679, 677, 20, 100000006, 678, 24, 682, 100000009, 20, 100000001, 680, 24, 681, 100000009, 25,
                                                  100000005, 682, 27, 684, 683, 35, 100000010, 100000009, 35, 100000010, 697, 19, 708, 686, 20, 707, 687, 24, 690, 688,
                                                  27, 689, 691, 28, 703, 694, 27, 695, 691, 28, 692, 694, 34, 693, 694, 35, 706, 694, 39, 100000002, 100000009, 28,
                                                  703, 696, 39, 100000002, 697, 47, 702, 698, 50, 702, 699, 65, 702, 700, 67, 702, 701, 68, 702, 100000012, 76,
                                                  100000011, 100000012, 34, 704, 705, 35, 706, 705, 39, 100000002, 100000008, 39, 100000002, 100000007, 39, 100000002,
                                                  100000004, 20, 100000001, 709, 39, 100000002, 100000003 };

    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams params) {
        Evaluator evaluator = new Evaluator();
        evaluator.region = params.region() == null ? null : params.region().id();
        evaluator.bucket = params.bucket();
        evaluator.useFIPS = params.useFips();
        evaluator.useDualStack = params.useDualStack();
        evaluator.endpoint = params.endpoint();
        evaluator.forcePathStyle = params.forcePathStyle();
        evaluator.accelerate = params.accelerate();
        evaluator.useGlobalEndpoint = params.useGlobalEndpoint();
        evaluator.useObjectLambdaEndpoint = params.useObjectLambdaEndpoint();
        evaluator.key = params.key();
        evaluator.prefix = params.prefix();
        evaluator.copySource = params.copySource();
        evaluator.disableAccessPoints = params.disableAccessPoints();
        evaluator.disableMultiRegionAccessPoints = params.disableMultiRegionAccessPoints();
        evaluator.useArnRegion = params.useArnRegion();
        evaluator.useS3ExpressControlEndpoint = params.useS3ExpressControlEndpoint();
        evaluator.disableS3ExpressSessionAuth = params.disableS3ExpressSessionAuth();
        final int[] bdd = BDD_DEFINITION;
        int nodeRef = 2;
        while ((nodeRef > 1 || nodeRef < -1) && nodeRef < 100000000) {
            int base = (Math.abs(nodeRef) - 1) * 3;
            int complemented = nodeRef >> 31 & 1; // 1 if complemented edge, else 0;
            int conditionResult = evaluator.cond(bdd[base]) ? 1 : 0;
            nodeRef = bdd[base + 2 - (complemented ^ conditionResult)];
        }
        if (nodeRef == -1 || nodeRef == 1) {
            return CompletableFutureUtils.failedFuture(SdkClientException
                                                           .create("Rule engine did not reach an error or endpoint result"));
        } else {
            RuleResult result = evaluator.result(nodeRef - 100000001);
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

    private static class Evaluator {
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

        String s3e_auth;

        String bucketAliasSuffix;

        String regionPrefix;

        String outpostId_ssa_2;

        String hardwareType;

        String s3e_fips;

        RulePartition regionPartition;

        String s3e_ds;

        RulePartition partitionResult;

        RuleUrl url;

        RuleArn bucketArn;

        String uri_encoded_bucket;

        String arnType;

        String accessPointName_ssa_1;

        String outpostId_ssa_1;

        RulePartition bucketPartition;

        String s3expressAvailabilityZoneId;

        String outpostType;

        String accessPointName_ssa_2;

        RulePartition mrapPartition;

        public boolean cond(int i) {
            switch (i) {
                case 0: {
                    return (region != null);
                }
                case 1: {
                    return (bucket != null);
                }
                case 2: {
                    return (RulesFunctions.awsParseArn(bucket) != null);
                }
                case 3: {
                    return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 6, true), "")));
                }
                case 4: {
                    return (useS3ExpressControlEndpoint != null);
                }
                case 5: {
                    return (Boolean.FALSE != useS3ExpressControlEndpoint);
                }
                case 6: {
                    return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 7, true), "")));
                }
                case 7: {
                    return (disableS3ExpressSessionAuth != null);
                }
                case 8: {
                    s3e_auth = RulesFunctions.ite(RulesFunctions.coalesce(disableS3ExpressSessionAuth, false), "sigv4",
                                                  "sigv4-s3express");
                    return s3e_auth != null;
                }
                case 9: {
                    return (Boolean.FALSE != disableS3ExpressSessionAuth);
                }
                case 10: {
                    bucketAliasSuffix = RulesFunctions.substring(bucket, 0, 7, true);
                    return bucketAliasSuffix != null;
                }
                case 11: {
                    return ("--op-s3".equals(bucketAliasSuffix));
                }
                case 12: {
                    regionPrefix = RulesFunctions.substring(bucket, 8, 12, true);
                    return regionPrefix != null;
                }
                case 13: {
                    outpostId_ssa_2 = RulesFunctions.substring(bucket, 32, 49, true);
                    return outpostId_ssa_2 != null;
                }
                case 14: {
                    hardwareType = RulesFunctions.substring(bucket, 49, 50, true);
                    return hardwareType != null;
                }
                case 15: {
                    s3e_fips = RulesFunctions.ite(useFIPS, "-fips", "");
                    return s3e_fips != null;
                }
                case 16: {
                    regionPartition = RulesFunctions.awsPartition(region);
                    return regionPartition != null;
                }
                case 17: {
                    s3e_ds = RulesFunctions.ite(useDualStack, ".dualstack", "");
                    return s3e_ds != null;
                }
                case 18: {
                    return (endpoint != null);
                }
                case 19: {
                    return (useFIPS);
                }
                case 20: {
                    return (accelerate);
                }
                case 21: {
                    return (RulesFunctions.parseURL(endpoint) != null);
                }
                case 22: {
                    return (forcePathStyle);
                }
                case 23: {
                    return (RulesFunctions.awsParseArn(bucket) != null);
                }
                case 24: {
                    partitionResult = RulesFunctions.awsPartition(region);
                    return partitionResult != null;
                }
                case 25: {
                    return ("aws-cn".equals(partitionResult.name()));
                }
                case 26: {
                    return (RulesFunctions.isValidHostLabel(outpostId_ssa_2, false));
                }
                case 27: {
                    return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, false));
                }
                case 28: {
                    url = RulesFunctions.parseURL(endpoint);
                    return url != null;
                }
                case 29: {
                    return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, true));
                }
                case 30: {
                    return ("http".equals(url.scheme()));
                }
                case 31: {
                    bucketArn = RulesFunctions.awsParseArn(bucket);
                    return bucketArn != null;
                }
                case 32: {
                    return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 4, false), "")));
                }
                case 33: {
                    return (RulesFunctions.isValidHostLabel(region, false));
                }
                case 34: {
                    return (url.isIp());
                }
                case 35: {
                    uri_encoded_bucket = RulesFunctions.uriEncode(bucket);
                    return uri_encoded_bucket != null;
                }
                case 36: {
                    return (useObjectLambdaEndpoint != null);
                }
                case 37: {
                    return (Boolean.FALSE != useObjectLambdaEndpoint);
                }
                case 38: {
                    return (RulesFunctions.isValidHostLabel(region, true));
                }
                case 39: {
                    return (useDualStack);
                }
                case 40: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 21, 23, true), "")));
                }
                case 41: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 16, 18, true), "")));
                }
                case 42: {
                    return (disableAccessPoints != null);
                }
                case 43: {
                    return (Boolean.FALSE != disableAccessPoints);
                }
                case 44: {
                    return (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null);
                }
                case 45: {
                    return (RulesFunctions.listAccess(bucketArn.resourceId(), 2) != null);
                }
                case 46: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 27, 29, true), "")));
                }
                case 47: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 19, 21, true), "")));
                }
                case 48: {
                    return ("e".equals(hardwareType));
                }
                case 49: {
                    return ("o".equals(hardwareType));
                }
                case 50: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 26, 28, true), "")));
                }
                case 51: {
                    arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
                    return arnType != null;
                }
                case 52: {
                    return ("".equals(arnType));
                }
                case 53: {
                    return ("accesspoint".equals(arnType));
                }
                case 54: {
                    accessPointName_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
                    return accessPointName_ssa_1 != null;
                }
                case 55: {
                    return ("".equals(accessPointName_ssa_1));
                }
                case 56: {
                    return ("s3-object-lambda".equals(bucketArn.service()));
                }
                case 57: {
                    return ("s3-outposts".equals(bucketArn.service()));
                }
                case 58: {
                    outpostId_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
                    return outpostId_ssa_1 != null;
                }
                case 59: {
                    return ("".equals(bucketArn.region()));
                }
                case 60: {
                    bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
                    return bucketPartition != null;
                }
                case 61: {
                    return (useArnRegion != null);
                }
                case 62: {
                    return (RulesFunctions.stringEquals(region, bucketArn.region()));
                }
                case 63: {
                    return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, true));
                }
                case 64: {
                    return (!url.isIp());
                }
                case 65: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 14, 16, true), "")));
                }
                case 66: {
                    return ("aws-global".equals(region));
                }
                case 67: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 20, 22, true), "")));
                }
                case 68: {
                    return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 15, 17, true), "")));
                }
                case 69: {
                    return (useGlobalEndpoint);
                }
                case 70: {
                    return (!useArnRegion);
                }
                case 71: {
                    return (RulesFunctions.isValidHostLabel(outpostId_ssa_1, false));
                }
                case 72: {
                    return (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name()));
                }
                case 73: {
                    return (RulesFunctions.isValidHostLabel(bucketArn.region(), true));
                }
                case 74: {
                    return ("us-east-1".equals(region));
                }
                case 75: {
                    return ("s3".equals(bucketArn.service()));
                }
                case 76: {
                    s3expressAvailabilityZoneId = RulesFunctions.listAccess(RulesFunctions.split(bucket, "--", 0), 1);
                    return s3expressAvailabilityZoneId != null;
                }
                case 77: {
                    return ("".equals(bucketArn.accountId()));
                }
                case 78: {
                    return (disableMultiRegionAccessPoints);
                }
                case 79: {
                    return (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false));
                }
                case 80: {
                    return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, false));
                }
                case 81: {
                    outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
                    return outpostType != null;
                }
                case 82: {
                    accessPointName_ssa_2 = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
                    return accessPointName_ssa_2 != null;
                }
                case 83: {
                    mrapPartition = RulesFunctions.awsPartition(region);
                    return mrapPartition != null;
                }
                case 84: {
                    return ("accesspoint".equals(outpostType));
                }
                case 85: {
                    return (RulesFunctions.stringEquals(bucketArn.partition(), mrapPartition.name()));
                }
                case 86: {
                    return ("beta".equals(regionPrefix));
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        public RuleResult result(int i) {
            switch (i) {
                case 0: {
                    return RuleResult.error("Accelerate cannot be used with FIPS");
                }
                case 1: {
                    return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
                }
                case 2: {
                    return RuleResult.error("A custom endpoint cannot be combined with FIPS");
                }
                case 3: {
                    return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
                }
                case 4: {
                    return RuleResult.error("Partition does not support FIPS");
                }
                case 5: {
                    return RuleResult.error("S3Express does not support S3 Accelerate.");
                }
                case 6: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + "/" + uri_encoded_bucket + url.path()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                                 Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
                }
                case 7: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                                 Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
                }
                case 8: {
                    return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
                }
                case 9: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3express-control" + s3e_fips + s3e_ds + "." + region + "."
                                                                   + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
                case 10: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3express" + s3e_fips + "-" + s3expressAvailabilityZoneId
                                                                   + s3e_ds + "." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                                 Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
                }
                case 11: {
                    return RuleResult.error("Unrecognized S3Express bucket name format.");
                }
                case 12: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                                 Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
                }
                case 13: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3express-control" + s3e_fips + s3e_ds + "." + region + "."
                                                                   + partitionResult.dnsSuffix()))
                                                   .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3express")
                                                                                    .signingRegion(region).build())).build());
                }
                case 14: {
                    return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
                }
                case 15: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".ec2." + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                                   .build());
                }
                case 16: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".ec2.s3-outposts." + region + "." + regionPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                                   .build());
                }
                case 17: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".op-" + outpostId_ssa_2 + "." + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                                   .build());
                }
                case 18: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".op-" + outpostId_ssa_2 + ".s3-outposts." + region + "."
                                                                   + regionPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(region).build()))
                                                   .build());
                }
                case 19: {
                    return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType
                                            + "\"");
                }
                case 20: {
                    return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
                }
                case 21: {
                    return RuleResult.error("Custom endpoint `" + endpoint + "` was not a valid URI");
                }
                case 22: {
                    return RuleResult.error("S3 Accelerate cannot be used in this region");
                }
                case 23: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 24: {
                    return RuleResult
                        .endpoint(Endpoint
                                      .builder()
                                      .url(URI.create("https://" + bucket + ".s3-fips.dualstack." + region + "."
                                                      + partitionResult.dnsSuffix()))
                                      .putAttribute(
                                          AwsEndpointAttribute.AUTH_SCHEMES,
                                          Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                       .signingRegion(region).build())).build());
                }
                case 25: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 26: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 27: {
                    return RuleResult
                        .endpoint(Endpoint
                                      .builder()
                                      .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack.us-east-1."
                                                      + partitionResult.dnsSuffix()))
                                      .putAttribute(
                                          AwsEndpointAttribute.AUTH_SCHEMES,
                                          Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                       .signingRegion("us-east-1").build())).build());
                }
                case 28: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 29: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 30: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 31: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 32: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 33: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 34: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 35: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 36: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 37: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 38: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 39: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + bucket + ".s3." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 40: {
                    return RuleResult.error("Invalid region: region was not a valid DNS name.");
                }
                case 41: {
                    return RuleResult.error("S3 Object Lambda does not support Dual-stack");
                }
                case 42: {
                    return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
                }
                case 43: {
                    return RuleResult.error("Access points are not supported for this operation");
                }
                case 44: {
                    return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                            + "` does not match client region `" + region + "` and UseArnRegion is `false`");
                }
                case 45: {
                    return RuleResult.error("Invalid ARN: Missing account id");
                }
                case 46: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                                   + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
                }
                case 47: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                                   + ".s3-object-lambda-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
                }
                case 48: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda."
                                                                   + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(bucketArn.region()).build())).build());
                }
                case 49: {
                    return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                            + accessPointName_ssa_1 + "`");
                }
                case 50: {
                    return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                            + bucketArn.accountId() + "`");
                }
                case 51: {
                    return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
                }
                case 52: {
                    return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`"
                                            + bucket + "`) has `" + bucketPartition.name() + "`");
                }
                case 53: {
                    return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
                }
                case 54: {
                    return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
                }
                case 55: {
                    return RuleResult
                        .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
                }
                case 56: {
                    return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                            + arnType + "`");
                }
                case 57: {
                    return RuleResult.error("Access Points do not support S3 Accelerate");
                }
                case 58: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                                   + ".s3-accesspoint-fips.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
                case 59: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                                   + ".s3-accesspoint-fips." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
                case 60: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                                   + ".s3-accesspoint.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
                case 61: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                                   + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
                case 62: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint."
                                                                   + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(bucketArn.region()).build())).build());
                }
                case 63: {
                    return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
                }
                case 64: {
                    return RuleResult.error("S3 MRAP does not support dual-stack");
                }
                case 65: {
                    return RuleResult.error("S3 MRAP does not support FIPS");
                }
                case 66: {
                    return RuleResult.error("S3 MRAP does not support S3 Accelerate");
                }
                case 67: {
                    return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
                }
                case 68: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_1 + ".accesspoint.s3-global."
                                                                   + mrapPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                     .signingRegionSet(Arrays.asList("*")).build())).build());
                }
                case 69: {
                    return RuleResult.error("Client was configured for partition `" + mrapPartition.name()
                                            + "` but bucket referred to partition `" + bucketArn.partition() + "`");
                }
                case 70: {
                    return RuleResult.error("Invalid Access Point Name");
                }
                case 71: {
                    return RuleResult.error("S3 Outposts does not support Dual-stack");
                }
                case 72: {
                    return RuleResult.error("S3 Outposts does not support FIPS");
                }
                case 73: {
                    return RuleResult.error("S3 Outposts does not support S3 Accelerate");
                }
                case 74: {
                    return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
                }
                case 75: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1
                                                                   + "." + url.authority()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region())
                                                                                                                                                   .build())).build());
                }
                case 76: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://" + accessPointName_ssa_2 + "-" + bucketArn.accountId() + "." + outpostId_ssa_1
                                                                   + ".s3-outposts." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3-outposts")
                                                                                     .signingRegionSet(Arrays.asList("*")).build(), SigV4AuthScheme.builder()
                                                                                                                                                   .disableDoubleEncoding(true).signingName("s3-outposts").signingRegion(bucketArn.region())
                                                                                                                                                   .build())).build());
                }
                case 77: {
                    return RuleResult.error("Expected an outpost type `accesspoint`, found " + outpostType);
                }
                case 78: {
                    return RuleResult.error("Invalid ARN: expected an access point name");
                }
                case 79: {
                    return RuleResult.error("Invalid ARN: Expected a 4-component resource");
                }
                case 80: {
                    return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                            + outpostId_ssa_1 + "`");
                }
                case 81: {
                    return RuleResult.error("Invalid ARN: The Outpost Id was not set");
                }
                case 82: {
                    return RuleResult.error("Invalid ARN: Unrecognized format: " + bucket + " (type: " + arnType + ")");
                }
                case 83: {
                    return RuleResult.error("Invalid ARN: No ARN type specified");
                }
                case 84: {
                    return RuleResult.error("Invalid ARN: `" + bucket + "` was not a valid ARN");
                }
                case 85: {
                    return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
                }
                case 86: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/"
                                                                   + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 87: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                                   + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 88: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.us-east-1." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 89: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips." + region + "." + partitionResult.dnsSuffix() + "/"
                                                                   + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 90: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/"
                                                                   + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 91: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                                   + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 92: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 93: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 94: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 95: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 96: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + region + "." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 97: {
                    return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
                }
                case 98: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(region).build())).build());
                }
                case 99: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-object-lambda-fips." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(region).build())).build());
                }
                case 100: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-object-lambda." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true)
                                                                                    .signingName("s3-object-lambda").signingRegion(region).build())).build());
                }
                case 101: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 102: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 103: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 104: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 105: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 106: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 107: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 108: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 109: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion("us-east-1").build())).build());
                }
                case 110: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 111: {
                    return RuleResult.endpoint(Endpoint
                                                   .builder()
                                                   .url(URI.create("https://s3." + region + "." + partitionResult.dnsSuffix()))
                                                   .putAttribute(
                                                       AwsEndpointAttribute.AUTH_SCHEMES,
                                                       Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                                    .signingRegion(region).build())).build());
                }
                case 112: {
                    return RuleResult.error("A region must be set when sending requests to S3.");
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }
    }

    public static class DynamicAuthBuilder {
        String name;

        private Map<String, String> properties = new HashMap<>();

        public static DynamicAuthBuilder builder() {
            return new DynamicAuthBuilder();
        }

        DynamicAuthBuilder name(String name) {
            this.name = name;
            return this;
        }

        DynamicAuthBuilder property(String key, String value) {
            properties.put(key, value);
            return this;
        }

        public EndpointAuthScheme build() {
            return null;
        }
    }
}
