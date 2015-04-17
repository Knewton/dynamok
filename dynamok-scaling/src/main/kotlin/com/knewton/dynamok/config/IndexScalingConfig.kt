/*
 * Copyright 2015 Knewton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.knewton.dynamok.config

import com.knewton.dynamok.data.DynamoIndex

/**
 * Configuration used for setting the scaling options of a particular index
 */
public data class IndexScalingConfig(
        // the index (primary or global secondary index) to configure
        val index: DynamoIndex,
        // the minimum allowed reads
        val minRead: Int = 5,
        // the maximum allowed reads
        val maxRead: Int = 50,
        // the minimum allowed writes
        val minWrite: Int = 5,
        // the maximum allowed writes
        val maxWrite: Int = 50,
        // if true, up-scaling provisioning is allowed
        val enableUpscale: Boolean = true,
        // if true, down-scaling provisioning is allowed
        val enableDownscale: Boolean = true,
        // the percent of consumed capacity before triggering an upscale
        val upscalePercent: Double = 0.85,
        // the percent of consumed capacity before triggering a downscale
        val downscalePercent: Double = 0.15,
        // amount to increase provisioning by (new = (current * (1 + factor)))
        val scaleUpFactor: Double = 0.50,
        // amount to decrease provisioning by (new = (current * (1 - factor)))
        val scaleDownFactor: Double = 0.80,
        // how long to wait between the last up or downscale before downscaling (used to throttle
        // downscales since AWS only allows four downscales every 24 hours)
        val downscaleWaitMinutes: Int = 60)