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
// uses new ite/split w/ express results reduced (requires dynamic auth builder)
// uses new Evaluator class with cond/result functions
// result and condition functions w/ switch dispatch
public final class BddCostOpt4Runtime6c implements S3EndpointProvider {
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

    private static final class Evaluator {
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

        public final boolean cond(int i) {
            switch (i) {
                case 0: {
                    return cond0();
                }
                case 1: {
                    return cond1();
                }
                case 2: {
                    return cond2();
                }
                case 3: {
                    return cond3();
                }
                case 4: {
                    return cond4();
                }
                case 5: {
                    return cond5();
                }
                case 6: {
                    return cond6();
                }
                case 7: {
                    return cond7();
                }
                case 8: {
                    return cond8();
                }
                case 9: {
                    return cond9();
                }
                case 10: {
                    return cond10();
                }
                case 11: {
                    return cond11();
                }
                case 12: {
                    return cond12();
                }
                case 13: {
                    return cond13();
                }
                case 14: {
                    return cond14();
                }
                case 15: {
                    return cond15();
                }
                case 16: {
                    return cond16();
                }
                case 17: {
                    return cond17();
                }
                case 18: {
                    return cond18();
                }
                case 19: {
                    return cond19();
                }
                case 20: {
                    return cond20();
                }
                case 21: {
                    return cond21();
                }
                case 22: {
                    return cond22();
                }
                case 23: {
                    return cond23();
                }
                case 24: {
                    return cond24();
                }
                case 25: {
                    return cond25();
                }
                case 26: {
                    return cond26();
                }
                case 27: {
                    return cond27();
                }
                case 28: {
                    return cond28();
                }
                case 29: {
                    return cond29();
                }
                case 30: {
                    return cond30();
                }
                case 31: {
                    return cond31();
                }
                case 32: {
                    return cond32();
                }
                case 33: {
                    return cond33();
                }
                case 34: {
                    return cond34();
                }
                case 35: {
                    return cond35();
                }
                case 36: {
                    return cond36();
                }
                case 37: {
                    return cond37();
                }
                case 38: {
                    return cond38();
                }
                case 39: {
                    return cond39();
                }
                case 40: {
                    return cond40();
                }
                case 41: {
                    return cond41();
                }
                case 42: {
                    return cond42();
                }
                case 43: {
                    return cond43();
                }
                case 44: {
                    return cond44();
                }
                case 45: {
                    return cond45();
                }
                case 46: {
                    return cond46();
                }
                case 47: {
                    return cond47();
                }
                case 48: {
                    return cond48();
                }
                case 49: {
                    return cond49();
                }
                case 50: {
                    return cond50();
                }
                case 51: {
                    return cond51();
                }
                case 52: {
                    return cond52();
                }
                case 53: {
                    return cond53();
                }
                case 54: {
                    return cond54();
                }
                case 55: {
                    return cond55();
                }
                case 56: {
                    return cond56();
                }
                case 57: {
                    return cond57();
                }
                case 58: {
                    return cond58();
                }
                case 59: {
                    return cond59();
                }
                case 60: {
                    return cond60();
                }
                case 61: {
                    return cond61();
                }
                case 62: {
                    return cond62();
                }
                case 63: {
                    return cond63();
                }
                case 64: {
                    return cond64();
                }
                case 65: {
                    return cond65();
                }
                case 66: {
                    return cond66();
                }
                case 67: {
                    return cond67();
                }
                case 68: {
                    return cond68();
                }
                case 69: {
                    return cond69();
                }
                case 70: {
                    return cond70();
                }
                case 71: {
                    return cond71();
                }
                case 72: {
                    return cond72();
                }
                case 73: {
                    return cond73();
                }
                case 74: {
                    return cond74();
                }
                case 75: {
                    return cond75();
                }
                case 76: {
                    return cond76();
                }
                case 77: {
                    return cond77();
                }
                case 78: {
                    return cond78();
                }
                case 79: {
                    return cond79();
                }
                case 80: {
                    return cond80();
                }
                case 81: {
                    return cond81();
                }
                case 82: {
                    return cond82();
                }
                case 83: {
                    return cond83();
                }
                case 84: {
                    return cond84();
                }
                case 85: {
                    return cond85();
                }
                case 86: {
                    return cond86();
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        public final RuleResult result(int i) {
            switch (i) {
                case 0: {
                    return result0();
                }
                case 1: {
                    return result1();
                }
                case 2: {
                    return result2();
                }
                case 3: {
                    return result3();
                }
                case 4: {
                    return result4();
                }
                case 5: {
                    return result5();
                }
                case 6: {
                    return result6();
                }
                case 7: {
                    return result7();
                }
                case 8: {
                    return result8();
                }
                case 9: {
                    return result9();
                }
                case 10: {
                    return result10();
                }
                case 11: {
                    return result11();
                }
                case 12: {
                    return result12();
                }
                case 13: {
                    return result13();
                }
                case 14: {
                    return result14();
                }
                case 15: {
                    return result15();
                }
                case 16: {
                    return result16();
                }
                case 17: {
                    return result17();
                }
                case 18: {
                    return result18();
                }
                case 19: {
                    return result19();
                }
                case 20: {
                    return result20();
                }
                case 21: {
                    return result21();
                }
                case 22: {
                    return result22();
                }
                case 23: {
                    return result23();
                }
                case 24: {
                    return result24();
                }
                case 25: {
                    return result25();
                }
                case 26: {
                    return result26();
                }
                case 27: {
                    return result27();
                }
                case 28: {
                    return result28();
                }
                case 29: {
                    return result29();
                }
                case 30: {
                    return result30();
                }
                case 31: {
                    return result31();
                }
                case 32: {
                    return result32();
                }
                case 33: {
                    return result33();
                }
                case 34: {
                    return result34();
                }
                case 35: {
                    return result35();
                }
                case 36: {
                    return result36();
                }
                case 37: {
                    return result37();
                }
                case 38: {
                    return result38();
                }
                case 39: {
                    return result39();
                }
                case 40: {
                    return result40();
                }
                case 41: {
                    return result41();
                }
                case 42: {
                    return result42();
                }
                case 43: {
                    return result43();
                }
                case 44: {
                    return result44();
                }
                case 45: {
                    return result45();
                }
                case 46: {
                    return result46();
                }
                case 47: {
                    return result47();
                }
                case 48: {
                    return result48();
                }
                case 49: {
                    return result49();
                }
                case 50: {
                    return result50();
                }
                case 51: {
                    return result51();
                }
                case 52: {
                    return result52();
                }
                case 53: {
                    return result53();
                }
                case 54: {
                    return result54();
                }
                case 55: {
                    return result55();
                }
                case 56: {
                    return result56();
                }
                case 57: {
                    return result57();
                }
                case 58: {
                    return result58();
                }
                case 59: {
                    return result59();
                }
                case 60: {
                    return result60();
                }
                case 61: {
                    return result61();
                }
                case 62: {
                    return result62();
                }
                case 63: {
                    return result63();
                }
                case 64: {
                    return result64();
                }
                case 65: {
                    return result65();
                }
                case 66: {
                    return result66();
                }
                case 67: {
                    return result67();
                }
                case 68: {
                    return result68();
                }
                case 69: {
                    return result69();
                }
                case 70: {
                    return result70();
                }
                case 71: {
                    return result71();
                }
                case 72: {
                    return result72();
                }
                case 73: {
                    return result73();
                }
                case 74: {
                    return result74();
                }
                case 75: {
                    return result75();
                }
                case 76: {
                    return result76();
                }
                case 77: {
                    return result77();
                }
                case 78: {
                    return result78();
                }
                case 79: {
                    return result79();
                }
                case 80: {
                    return result80();
                }
                case 81: {
                    return result81();
                }
                case 82: {
                    return result82();
                }
                case 83: {
                    return result83();
                }
                case 84: {
                    return result84();
                }
                case 85: {
                    return result85();
                }
                case 86: {
                    return result86();
                }
                case 87: {
                    return result87();
                }
                case 88: {
                    return result88();
                }
                case 89: {
                    return result89();
                }
                case 90: {
                    return result90();
                }
                case 91: {
                    return result91();
                }
                case 92: {
                    return result92();
                }
                case 93: {
                    return result93();
                }
                case 94: {
                    return result94();
                }
                case 95: {
                    return result95();
                }
                case 96: {
                    return result96();
                }
                case 97: {
                    return result97();
                }
                case 98: {
                    return result98();
                }
                case 99: {
                    return result99();
                }
                case 100: {
                    return result100();
                }
                case 101: {
                    return result101();
                }
                case 102: {
                    return result102();
                }
                case 103: {
                    return result103();
                }
                case 104: {
                    return result104();
                }
                case 105: {
                    return result105();
                }
                case 106: {
                    return result106();
                }
                case 107: {
                    return result107();
                }
                case 108: {
                    return result108();
                }
                case 109: {
                    return result109();
                }
                case 110: {
                    return result110();
                }
                case 111: {
                    return result111();
                }
                case 112: {
                    return result112();
                }
                default: {
                    throw new IllegalArgumentException("Unknown condition index");
                }
            }
        }

        private final RuleResult result0() {
            return RuleResult.error("Accelerate cannot be used with FIPS");
        }

        private final RuleResult result1() {
            return RuleResult.error("Cannot set dual-stack in combination with a custom endpoint.");
        }

        private final RuleResult result2() {
            return RuleResult.error("A custom endpoint cannot be combined with FIPS");
        }

        private final RuleResult result3() {
            return RuleResult.error("A custom endpoint cannot be combined with S3 Accelerate");
        }

        private final RuleResult result4() {
            return RuleResult.error("Partition does not support FIPS");
        }

        private final RuleResult result5() {
            return RuleResult.error("S3Express does not support S3 Accelerate.");
        }

        private final RuleResult result6() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + "/" + uri_encoded_bucket + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
        }

        private final RuleResult result7() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
        }

        private final RuleResult result8() {
            return RuleResult.error("S3Express bucket name is not a valid virtual hostable name.");
        }

        private final RuleResult result9() {
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

        private final RuleResult result10() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3express" + s3e_fips + "-" + s3expressAvailabilityZoneId + s3e_ds
                                                           + "." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
        }

        private final RuleResult result11() {
            return RuleResult.error("Unrecognized S3Express bucket name format.");
        }

        private final RuleResult result12() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(KnownS3ExpressEndpointProperty.BACKEND, "S3Express")
                                           .putAttribute(AwsEndpointAttribute.AUTH_SCHEMES,
                                                         Arrays.asList(DynamicAuthBuilder.builder().name(s3e_auth).build())).build());
        }

        private final RuleResult result13() {
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

        private final RuleResult result14() {
            return RuleResult.error("Expected a endpoint to be specified but no endpoint was found");
        }

        private final RuleResult result15() {
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

        private final RuleResult result16() {
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

        private final RuleResult result17() {
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

        private final RuleResult result18() {
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

        private final RuleResult result19() {
            return RuleResult.error("Unrecognized hardware type: \"Expected hardware type o or e but got " + hardwareType + "\"");
        }

        private final RuleResult result20() {
            return RuleResult.error("Invalid ARN: The outpost Id must only contain a-z, A-Z, 0-9 and `-`.");
        }

        private final RuleResult result21() {
            return RuleResult.error("Custom endpoint `" + endpoint + "` was not a valid URI");
        }

        private final RuleResult result22() {
            return RuleResult.error("S3 Accelerate cannot be used in this region");
        }

        private final RuleResult result23() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result24() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result25() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result26() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result27() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result28() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate.dualstack." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result29() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result30() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result31() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result32() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result33() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result34() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + bucket + "." + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result35() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result36() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3-accelerate." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result37() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result38() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result39() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + bucket + ".s3." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result40() {
            return RuleResult.error("Invalid region: region was not a valid DNS name.");
        }

        private final RuleResult result41() {
            return RuleResult.error("S3 Object Lambda does not support Dual-stack");
        }

        private final RuleResult result42() {
            return RuleResult.error("S3 Object Lambda does not support S3 Accelerate");
        }

        private final RuleResult result43() {
            return RuleResult.error("Access points are not supported for this operation");
        }

        private final RuleResult result44() {
            return RuleResult.error("Invalid configuration: region from ARN `" + bucketArn.region()
                                    + "` does not match client region `" + region + "` and UseArnRegion is `false`");
        }

        private final RuleResult result45() {
            return RuleResult.error("Invalid ARN: Missing account id");
        }

        private final RuleResult result46() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                           + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result47() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda-fips."
                                                           + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result48() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-object-lambda."
                                                           + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result49() {
            return RuleResult.error("Invalid ARN: The access point name may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + accessPointName_ssa_1 + "`");
        }

        private final RuleResult result50() {
            return RuleResult.error("Invalid ARN: The account id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + bucketArn.accountId() + "`");
        }

        private final RuleResult result51() {
            return RuleResult.error("Invalid region in ARN: `" + bucketArn.region() + "` (invalid DNS name)");
        }

        private final RuleResult result52() {
            return RuleResult.error("Client was configured for partition `" + partitionResult.name() + "` but ARN (`" + bucket
                                    + "`) has `" + bucketPartition.name() + "`");
        }

        private final RuleResult result53() {
            return RuleResult.error("Invalid ARN: The ARN may only contain a single resource component after `accesspoint`.");
        }

        private final RuleResult result54() {
            return RuleResult.error("Invalid ARN: bucket ARN is missing a region");
        }

        private final RuleResult result55() {
            return RuleResult
                .error("Invalid ARN: Expected a resource of the format `accesspoint:<accesspoint name>` but no name was provided");
        }

        private final RuleResult result56() {
            return RuleResult.error("Invalid ARN: Object Lambda ARNs only support `accesspoint` arn types, but found: `"
                                    + arnType + "`");
        }

        private final RuleResult result57() {
            return RuleResult.error("Access Points do not support S3 Accelerate");
        }

        private final RuleResult result58() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                           + ".s3-accesspoint-fips.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result59() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint-fips."
                                                           + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result60() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId()
                                                           + ".s3-accesspoint.dualstack." + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result61() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + "."
                                                           + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result62() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + "-" + bucketArn.accountId() + ".s3-accesspoint."
                                                           + bucketArn.region() + "." + bucketPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(bucketArn.region()).build())).build());
        }

        private final RuleResult result63() {
            return RuleResult.error("Invalid ARN: The ARN was not for the S3 service, found: " + bucketArn.service());
        }

        private final RuleResult result64() {
            return RuleResult.error("S3 MRAP does not support dual-stack");
        }

        private final RuleResult result65() {
            return RuleResult.error("S3 MRAP does not support FIPS");
        }

        private final RuleResult result66() {
            return RuleResult.error("S3 MRAP does not support S3 Accelerate");
        }

        private final RuleResult result67() {
            return RuleResult.error("Invalid configuration: Multi-Region Access Point ARNs are disabled.");
        }

        private final RuleResult result68() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://" + accessPointName_ssa_1 + ".accesspoint.s3-global." + mrapPartition.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4aAuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                             .signingRegionSet(Arrays.asList("*")).build())).build());
        }

        private final RuleResult result69() {
            return RuleResult.error("Client was configured for partition `" + mrapPartition.name()
                                    + "` but bucket referred to partition `" + bucketArn.partition() + "`");
        }

        private final RuleResult result70() {
            return RuleResult.error("Invalid Access Point Name");
        }

        private final RuleResult result71() {
            return RuleResult.error("S3 Outposts does not support Dual-stack");
        }

        private final RuleResult result72() {
            return RuleResult.error("S3 Outposts does not support FIPS");
        }

        private final RuleResult result73() {
            return RuleResult.error("S3 Outposts does not support S3 Accelerate");
        }

        private final RuleResult result74() {
            return RuleResult.error("Invalid Arn: Outpost Access Point ARN contains sub resources");
        }

        private final RuleResult result75() {
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

        private final RuleResult result76() {
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

        private final RuleResult result77() {
            return RuleResult.error("Expected an outpost type `accesspoint`, found " + outpostType);
        }

        private final RuleResult result78() {
            return RuleResult.error("Invalid ARN: expected an access point name");
        }

        private final RuleResult result79() {
            return RuleResult.error("Invalid ARN: Expected a 4-component resource");
        }

        private final RuleResult result80() {
            return RuleResult.error("Invalid ARN: The outpost Id may only contain a-z, A-Z, 0-9 and `-`. Found: `"
                                    + outpostId_ssa_1 + "`");
        }

        private final RuleResult result81() {
            return RuleResult.error("Invalid ARN: The Outpost Id was not set");
        }

        private final RuleResult result82() {
            return RuleResult.error("Invalid ARN: Unrecognized format: " + bucket + " (type: " + arnType + ")");
        }

        private final RuleResult result83() {
            return RuleResult.error("Invalid ARN: No ARN type specified");
        }

        private final RuleResult result84() {
            return RuleResult.error("Invalid ARN: `" + bucket + "` was not a valid ARN");
        }

        private final RuleResult result85() {
            return RuleResult.error("Path-style addressing cannot be used with ARN buckets");
        }

        private final RuleResult result86() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result87() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result88() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.us-east-1." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result89() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + region + "." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result90() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result91() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix() + "/"
                                                           + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result92() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result93() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.normalizedPath() + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result94() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result95() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result96() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + region + "." + partitionResult.dnsSuffix() + "/" + uri_encoded_bucket))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result97() {
            return RuleResult.error("Path-style addressing cannot be used with S3 Accelerate");
        }

        private final RuleResult result98() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result99() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-object-lambda-fips." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result100() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-object-lambda." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3-object-lambda")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result101() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result102() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result103() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result104() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3-fips." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result105() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack.us-east-1." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result106() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3.dualstack." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result107() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result108() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create(url.scheme() + "://" + url.authority() + url.path()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result109() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion("us-east-1").build())).build());
        }

        private final RuleResult result110() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result111() {
            return RuleResult.endpoint(Endpoint
                                           .builder()
                                           .url(URI.create("https://s3." + region + "." + partitionResult.dnsSuffix()))
                                           .putAttribute(
                                               AwsEndpointAttribute.AUTH_SCHEMES,
                                               Arrays.asList(SigV4AuthScheme.builder().disableDoubleEncoding(true).signingName("s3")
                                                                            .signingRegion(region).build())).build());
        }

        private final RuleResult result112() {
            return RuleResult.error("A region must be set when sending requests to S3.");
        }

        private final boolean cond0() {
            return (region != null);
        }

        private final boolean cond1() {
            return (bucket != null);
        }

        private final boolean cond2() {
            return (RulesFunctions.awsParseArn(bucket) != null);
        }

        private final boolean cond3() {
            return ("--x-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 6, true), "")));
        }

        private final boolean cond4() {
            return (useS3ExpressControlEndpoint != null);
        }

        private final boolean cond5() {
            return (Boolean.FALSE != useS3ExpressControlEndpoint);
        }

        private final boolean cond6() {
            return ("--xa-s3".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 7, true), "")));
        }

        private final boolean cond7() {
            return (disableS3ExpressSessionAuth != null);
        }

        private final boolean cond8() {
            s3e_auth = RulesFunctions
                .ite(RulesFunctions.coalesce(disableS3ExpressSessionAuth, false), "sigv4", "sigv4-s3express");
            return s3e_auth != null;
        }

        private final boolean cond9() {
            return (Boolean.FALSE != disableS3ExpressSessionAuth);
        }

        private final boolean cond10() {
            bucketAliasSuffix = RulesFunctions.substring(bucket, 0, 7, true);
            return bucketAliasSuffix != null;
        }

        private final boolean cond11() {
            return ("--op-s3".equals(bucketAliasSuffix));
        }

        private final boolean cond12() {
            regionPrefix = RulesFunctions.substring(bucket, 8, 12, true);
            return regionPrefix != null;
        }

        private final boolean cond13() {
            outpostId_ssa_2 = RulesFunctions.substring(bucket, 32, 49, true);
            return outpostId_ssa_2 != null;
        }

        private final boolean cond14() {
            hardwareType = RulesFunctions.substring(bucket, 49, 50, true);
            return hardwareType != null;
        }

        private final boolean cond15() {
            s3e_fips = RulesFunctions.ite(useFIPS, "-fips", "");
            return s3e_fips != null;
        }

        private final boolean cond16() {
            regionPartition = RulesFunctions.awsPartition(region);
            return regionPartition != null;
        }

        private final boolean cond17() {
            s3e_ds = RulesFunctions.ite(useDualStack, ".dualstack", "");
            return s3e_ds != null;
        }

        private final boolean cond18() {
            return (endpoint != null);
        }

        private final boolean cond19() {
            return (useFIPS);
        }

        private final boolean cond20() {
            return (accelerate);
        }

        private final boolean cond21() {
            return (RulesFunctions.parseURL(endpoint) != null);
        }

        private final boolean cond22() {
            return (forcePathStyle);
        }

        private final boolean cond23() {
            return (RulesFunctions.awsParseArn(bucket) != null);
        }

        private final boolean cond24() {
            partitionResult = RulesFunctions.awsPartition(region);
            return partitionResult != null;
        }

        private final boolean cond25() {
            return ("aws-cn".equals(partitionResult.name()));
        }

        private final boolean cond26() {
            return (RulesFunctions.isValidHostLabel(outpostId_ssa_2, false));
        }

        private final boolean cond27() {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, false));
        }

        private final boolean cond28() {
            url = RulesFunctions.parseURL(endpoint);
            return url != null;
        }

        private final boolean cond29() {
            return (RulesFunctions.awsIsVirtualHostableS3Bucket(bucket, true));
        }

        private final boolean cond30() {
            return ("http".equals(url.scheme()));
        }

        private final boolean cond31() {
            bucketArn = RulesFunctions.awsParseArn(bucket);
            return bucketArn != null;
        }

        private final boolean cond32() {
            return ("arn:".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 0, 4, false), "")));
        }

        private final boolean cond33() {
            return (RulesFunctions.isValidHostLabel(region, false));
        }

        private final boolean cond34() {
            return (url.isIp());
        }

        private final boolean cond35() {
            uri_encoded_bucket = RulesFunctions.uriEncode(bucket);
            return uri_encoded_bucket != null;
        }

        private final boolean cond36() {
            return (useObjectLambdaEndpoint != null);
        }

        private final boolean cond37() {
            return (Boolean.FALSE != useObjectLambdaEndpoint);
        }

        private final boolean cond38() {
            return (RulesFunctions.isValidHostLabel(region, true));
        }

        private final boolean cond39() {
            return (useDualStack);
        }

        private final boolean cond40() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 21, 23, true), "")));
        }

        private final boolean cond41() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 16, 18, true), "")));
        }

        private final boolean cond42() {
            return (disableAccessPoints != null);
        }

        private final boolean cond43() {
            return (Boolean.FALSE != disableAccessPoints);
        }

        private final boolean cond44() {
            return (RulesFunctions.listAccess(bucketArn.resourceId(), 4) != null);
        }

        private final boolean cond45() {
            return (RulesFunctions.listAccess(bucketArn.resourceId(), 2) != null);
        }

        private final boolean cond46() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 27, 29, true), "")));
        }

        private final boolean cond47() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 19, 21, true), "")));
        }

        private final boolean cond48() {
            return ("e".equals(hardwareType));
        }

        private final boolean cond49() {
            return ("o".equals(hardwareType));
        }

        private final boolean cond50() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 26, 28, true), "")));
        }

        private final boolean cond51() {
            arnType = RulesFunctions.listAccess(bucketArn.resourceId(), 0);
            return arnType != null;
        }

        private final boolean cond52() {
            return ("".equals(arnType));
        }

        private final boolean cond53() {
            return ("accesspoint".equals(arnType));
        }

        private final boolean cond54() {
            accessPointName_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
            return accessPointName_ssa_1 != null;
        }

        private final boolean cond55() {
            return ("".equals(accessPointName_ssa_1));
        }

        private final boolean cond56() {
            return ("s3-object-lambda".equals(bucketArn.service()));
        }

        private final boolean cond57() {
            return ("s3-outposts".equals(bucketArn.service()));
        }

        private final boolean cond58() {
            outpostId_ssa_1 = RulesFunctions.listAccess(bucketArn.resourceId(), 1);
            return outpostId_ssa_1 != null;
        }

        private final boolean cond59() {
            return ("".equals(bucketArn.region()));
        }

        private final boolean cond60() {
            bucketPartition = RulesFunctions.awsPartition(bucketArn.region());
            return bucketPartition != null;
        }

        private final boolean cond61() {
            return (useArnRegion != null);
        }

        private final boolean cond62() {
            return (RulesFunctions.stringEquals(region, bucketArn.region()));
        }

        private final boolean cond63() {
            return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, true));
        }

        private final boolean cond64() {
            return (!url.isIp());
        }

        private final boolean cond65() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 14, 16, true), "")));
        }

        private final boolean cond66() {
            return ("aws-global".equals(region));
        }

        private final boolean cond67() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 20, 22, true), "")));
        }

        private final boolean cond68() {
            return ("--".equals(RulesFunctions.coalesce(RulesFunctions.substring(bucket, 15, 17, true), "")));
        }

        private final boolean cond69() {
            return (useGlobalEndpoint);
        }

        private final boolean cond70() {
            return (!useArnRegion);
        }

        private final boolean cond71() {
            return (RulesFunctions.isValidHostLabel(outpostId_ssa_1, false));
        }

        private final boolean cond72() {
            return (RulesFunctions.stringEquals(bucketPartition.name(), partitionResult.name()));
        }

        private final boolean cond73() {
            return (RulesFunctions.isValidHostLabel(bucketArn.region(), true));
        }

        private final boolean cond74() {
            return ("us-east-1".equals(region));
        }

        private final boolean cond75() {
            return ("s3".equals(bucketArn.service()));
        }

        private final boolean cond76() {
            s3expressAvailabilityZoneId = RulesFunctions.listAccess(RulesFunctions.split(bucket, "--", 0), 1);
            return s3expressAvailabilityZoneId != null;
        }

        private final boolean cond77() {
            return ("".equals(bucketArn.accountId()));
        }

        private final boolean cond78() {
            return (disableMultiRegionAccessPoints);
        }

        private final boolean cond79() {
            return (RulesFunctions.isValidHostLabel(bucketArn.accountId(), false));
        }

        private final boolean cond80() {
            return (RulesFunctions.isValidHostLabel(accessPointName_ssa_1, false));
        }

        private final boolean cond81() {
            outpostType = RulesFunctions.listAccess(bucketArn.resourceId(), 2);
            return outpostType != null;
        }

        private final boolean cond82() {
            accessPointName_ssa_2 = RulesFunctions.listAccess(bucketArn.resourceId(), 3);
            return accessPointName_ssa_2 != null;
        }

        private final boolean cond83() {
            mrapPartition = RulesFunctions.awsPartition(region);
            return mrapPartition != null;
        }

        private final boolean cond84() {
            return ("accesspoint".equals(outpostType));
        }

        private final boolean cond85() {
            return (RulesFunctions.stringEquals(bucketArn.partition(), mrapPartition.name()));
        }

        private final boolean cond86() {
            return ("beta".equals(regionPrefix));
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
