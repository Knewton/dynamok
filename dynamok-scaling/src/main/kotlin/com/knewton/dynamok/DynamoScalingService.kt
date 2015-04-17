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

package com.knewton.dynamok

import com.amazonaws.AmazonClientException
import com.amazonaws.services.dynamodbv2.model.*
import com.knewton.dynamok.config.IndexScalingConfig
import com.knewton.dynamok.config.ScalingServiceConfig
import com.knewton.dynamok.connections.AWSClientFactory
import com.knewton.dynamok.connections.CloudWatchConnection
import com.knewton.dynamok.connections.DynamoConnection
import com.knewton.dynamok.connections.SNSConnection
import com.knewton.dynamok.data.DynamoIndex
import com.knewton.dynamok.data.DynamoIndexDescription
import org.joda.time.DateTime
import org.joda.time.Minutes
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.properties.Delegates

/**
 * Dynamo scaling service responsible for periodically polling consumed throughput for Dynamo
 * indices and updating the provisioned throughput if necessary.
 *
 * @param serviceConfig The service configuration (poll rate, notification settings, etc.)
 * @param clientFactory The AWS client factory used to create connections to SNS, CloudWatch, and
 *                      Dynamo
 */
open class DynamoScalingService(private val serviceConfig: ScalingServiceConfig,
                                clientFactory: AWSClientFactory) {

    private val dynamoConnection = DynamoConnection(clientFactory)
    private val snsConnection = SNSConnection(clientFactory)
    private val cloudWatchConnection = CloudWatchConnection(clientFactory)
    private val scalingConfigs = ConcurrentHashMap<DynamoIndex, IndexScalingConfig>()
    private val executor = Executors.newSingleThreadExecutor()
    private var threadFuture: Future<*> by Delegates.notNull()

    /**
     * Adds or replaces an existing scaling configuration.  The configuration will be read
     * in the next poll.
     *
     * @param config The configuration to add or replace
     */
    public open fun addIndex(config: IndexScalingConfig) {
        scalingConfigs.put(config.index, config)
    }

    /**
     * Removes an existing scaling configuration (stops updating it) and returns the old config.
     *
     * @param index The index (primary or global secondary index) to remove
     * @return The old configuration associated with the index, or null if the index was not found
     */
    public open fun removeIndex(index: DynamoIndex): IndexScalingConfig? {
        return scalingConfigs.remove(index)
    }

    /**
     * Starts the service (begins polling and adjusting Dynamo table provisioning)
     */
    public open fun start() {
        threadFuture = executor.submit {
            while (!Thread.interrupted()) {
                try {
                    scalingConfigs.values().forEach { checkAndUpdateProvisioning(it) }
                    Thread.sleep(serviceConfig.checkIntervalSeconds * 1000L)
                } catch (e: Exception) {
                    error("Dynamo Scaling - Unexpected Exception", e.toString())
                }
            }
        }
    }

    /**
     * Returns whether or not the service has been shut down
     *
     * @return True if the service has been shut down, false otherwise
     */
    public open fun isShutDown(): Boolean {
        return !executor.isShutdown()
    }

    /**
     * Stops the service - once stopped, the service cannot be restarted.
     */
    public open fun stop() {
        threadFuture.cancel(true)
        executor.shutdown()
    }

    /**
     * Checks the current consumed and provisioned capacity of a given index.  If necessary, will
     * also trigger an update to the provisioned capacity.
     *
     * @param config The index to check and update and its configuration
     * @throws AmazonClientException If the client request failed
     */
    open fun checkAndUpdateProvisioning(config: IndexScalingConfig) {
        val description = dynamoConnection.getDescription(config.index)
        val consumedReads = cloudWatchConnection.getConsumedReads(config.index)
        val consumedWrites = cloudWatchConnection.getConsumedWrites(config.index)
        val (targetReads, targetWrites) = computeTargetThroughput(config, description,
                                                                  consumedReads, consumedWrites)
        if (targetReads != description.readCapacity || targetWrites != description.writeCapacity) {
            updateProvisionedThroughput(config, description, targetReads, targetWrites,
                                        consumedReads, consumedWrites)
        }
    }

    /**
     * True if given the configuration and index description the provisioned throughput should be
     * increased (if up scaling is enabled and scaling is not overridden).
     *
     * @param config The index and configuration to check
     * @param description The current index throughput description
     * @return True if should upscale throughput, false otherwise
     */
    open fun shouldUpscale(config: IndexScalingConfig,
                           description: DynamoIndexDescription): Boolean {
        return config.enableUpscale && !isScalingOverridden(config, description)
    }

    /**
     * True if given the configuration and index description the provisioned throughput should be
     * decreased (if down scaling is enabled, scaling is not overridden, and the last update
     * was over config.downscaleWaitMinutes minutes ago).
     *
     * @param config The index and configuration to check
     * @param description The current index throughput description
     * @return True if should downscale throughput, false otherwise
     */
    open fun shouldDownscale(config: IndexScalingConfig,
                             description: DynamoIndexDescription): Boolean {
        val now = DateTime.now()
        val downscaleInterval = Minutes.minutesBetween(description.lastDecrease, now).getMinutes()
        val upscaleInterval = Minutes.minutesBetween(description.lastIncrease, now).getMinutes()
        return config.enableDownscale
               && !isScalingOverridden(config, description)
               && downscaleInterval > config.downscaleWaitMinutes
               && upscaleInterval > config.downscaleWaitMinutes
    }

    /**
     * Computes the new throughput to update to given the configuration and current consumed and
     * provisioned throughput.
     *
     * @param config The index and configuration to compute the new throughput for
     * @param description The current index throughput description
     * @param consumedReads The current consumed reads
     * @param consumedWrites The current consumed writes
     * @return A pair of values, where pair.first is the target read and pair.second is the target
     *         write throughput.
     */
    open fun computeTargetThroughput(config: IndexScalingConfig,
                                     description: DynamoIndexDescription, consumedReads: Double,
                                     consumedWrites: Double): Pair<Long, Long> {
        var targetReads = description.readCapacity
        var targetWrites = description.writeCapacity
        val percentConsumedReads = consumedReads / description.readCapacity.toDouble()
        val percentConsumedWrites = consumedWrites / description.writeCapacity.toDouble()

        if (shouldUpscale(config, description)) {
            if (percentConsumedReads >= config.upscalePercent) {
                targetReads = computeTargetUnit(targetReads, config.scaleUpFactor + 1,
                                                config.minRead, config.maxRead)
            }
            if (percentConsumedWrites >= config.upscalePercent) {
                targetWrites = computeTargetUnit(targetWrites, config.scaleUpFactor + 1,
                                                 config.minWrite, config.maxWrite)
            }
        }
        if (shouldDownscale(config, description)) {
            if (percentConsumedReads <= config.downscalePercent) {
                targetReads = computeTargetUnit(targetReads, 1 - config.scaleDownFactor,
                                                config.minRead, config.maxRead)
            }
            if (percentConsumedWrites <= config.downscalePercent) {
                targetWrites = computeTargetUnit(targetWrites, 1 - config.scaleDownFactor,
                                                 config.minWrite, config.maxWrite)
            }
        }

        return Pair(targetReads, targetWrites)
    }

    /**
     * Creates and runs an update request to Dynamo to update the given index to the new target
     * throughput.  This request will only go through if the current index status is ACTIVE.  If
     * the maximum provisioning is reached with this update, an error will be logged and a
     * notification will be sent to the configured ARN for this service.
     *
     * @param config The index and configuration to run the update on
     * @param description The current index throughput description
     * @param targetReads The new read provisioning value
     * @param targetWrites The new write provisioning value
     * @param consumedReads The consumed read provisioning
     * @param consumedWrites The consumed write provisioning
     * @throws AmazonClientException If the client request failed
     */
    open fun updateProvisionedThroughput(config: IndexScalingConfig,
                                         description: DynamoIndexDescription,
                                         targetReads: Long, targetWrites: Long,
                                         consumedReads: Double, consumedWrites: Double) {
        if (serviceConfig.notificationARN.isNotEmpty() &&
            (targetReads >= config.maxRead || targetWrites >= config.maxWrite)) {
            val message = "Scaling has reached maximum provisioning and cannot increase further. " +
                          "Index: ${config.index}, Consumed: (R: ${targetReads}, W: " +
                          "${targetWrites}), Config: (R: ${config.maxRead}, W: ${config.maxWrite}" +
                          ").  You should either modify the config or manually override the value" +
                          " in AWS."
            error("Dynamo Scaling - Maximum Reached", message)
        }

        if (description.status != TableStatus.ACTIVE.toString()) return
        val request = createUpdateRequest(config.index, targetReads, targetWrites)
        dynamoConnection.client.updateTable(request)
    }

    /**
     * Helper method to create an update request for a given index (since a different request must
     * be created depending on whether this is a primary index or a global secondary index).
     *
     * @param index The index to create the update request for
     * @param targetReads The new read provisioning value
     * @param targetWrites The new write provisioning value
     * @return An UpdateTableRequest which will update the index to the new provisioning
     */
    open fun createUpdateRequest(index: DynamoIndex, targetReads: Long,
                                 targetWrites: Long): UpdateTableRequest {
        val throughput = ProvisionedThroughput(targetReads, targetWrites)
        return if (index.gsiName.isNotEmpty()) {
            val gsiUpdateAction = UpdateGlobalSecondaryIndexAction()
                    .withIndexName(index.gsiName)
                    .withProvisionedThroughput(throughput)
            val gsiUpdate = GlobalSecondaryIndexUpdate().withUpdate(gsiUpdateAction)

            UpdateTableRequest().withTableName(index.tableName)
                                .withGlobalSecondaryIndexUpdates(gsiUpdate)
        } else {
            UpdateTableRequest().withTableName(index.tableName)
                                .withProvisionedThroughput(throughput)
        }
    }

    /**
     * Check if scaling has been manually overridden.  Scaling is overridden (scaling service
     * will not update provisioning) if the current provisioning is set out of bounds of the
     * configured minimums or maximums of reads or writes.  This can be done via the AWS Console.
     *
     * @param config The index and configuration to check for override
     * @param description The current index throughput description
     * @return True if scaling has been overridden, false otherwise
     */
    open fun isScalingOverridden(config: IndexScalingConfig,
                                 description: DynamoIndexDescription): Boolean {
        return description.readCapacity > config.maxRead
               || description.readCapacity < config.minRead
               || description.writeCapacity > config.maxWrite
               || description.writeCapacity < config.minWrite
    }

    /**
     * Computes the new provisioning to update to given the current provisioning by multiplying
     * by the scale factor and then clamping the result between min and max.
     *
     * @param currentUnits The current provisioning
     * @param scale The scale factor to multiply by
     * @param min The minimum allowed value (lower end of the clamp)
     * @param max The maximum allowed value (upper end of the clamp)
     * @return
     */
    open fun computeTargetUnit(currentUnits: Long, scale: Double, min: Int, max: Int): Long {
        return Math.max(Math.min(currentUnits * scale, max.toDouble()), min.toDouble()).toLong()
    }

    /**
     * Called on error.  Attempts to post a notification to SNS if the configured ARN is not
     * empty.  The error will also be logged.
     *
     * @param subject The subject of the error / SNS message
     * @param message The body of the error / SNS message
     */
    open fun error(subject: String, message: String) {
        if (serviceConfig.notificationARN.isNotEmpty()) {
            try {
                snsConnection.postNotification(serviceConfig.notificationARN, subject, message)
            } catch (e: AmazonClientException) {
                LOG.error("Failed to send SNS", e)
            }
        }
        LOG.error("($subject) $message")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(javaClass<DynamoScalingService>())
    }
}