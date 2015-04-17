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

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.model.Datapoint
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputDescription
import com.amazonaws.services.dynamodbv2.model.TableDescription
import com.amazonaws.services.sns.AmazonSNSClient
import com.knewton.dynamok.config.IndexScalingConfig
import com.knewton.dynamok.config.ScalingServiceConfig
import com.knewton.dynamok.connections.AWSClientFactory
import com.knewton.dynamok.data.DynamoIndex
import com.knewton.dynamok.data.DynamoIndexDescription
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.properties.Delegates
import org.mockito.Mockito.`when` as mWhen

class DynamoScalingServiceTest {

    val mockSNSClient = mock(javaClass<AmazonSNSClient>())
    val mockCloudWatchClient = mock(javaClass<AmazonCloudWatchClient>())
    val mockDynamoClient = mock(javaClass<AmazonDynamoDBClient>())
    val mockClientFactory = mock(javaClass<AWSClientFactory>())
    val serviceConfig = ScalingServiceConfig(notificationARN = "ARN")
    var underTest: DynamoScalingService by Delegates.notNull()

    init {
        mWhen(mockClientFactory.createSNSClient()).thenReturn(mockSNSClient)
        mWhen(mockClientFactory.createCloudWatchClient()).thenReturn(mockCloudWatchClient)
        mWhen(mockClientFactory.createDynamoClient()).thenReturn(mockDynamoClient)
        underTest = DynamoScalingService(serviceConfig, mockClientFactory)
    }


    Test
    fun testAddAndRemoveIndex() {
        val config = IndexScalingConfig(DynamoIndex("test"))
        underTest.addIndex(config)
        assertThat(underTest.removeIndex(config.index)).isEqualTo(config)
        assertThat(underTest.removeIndex(config.index)).isEqualTo(null)
    }

    Test
    fun testError() {
        underTest.error("hello", "world")
        verify(mockSNSClient).publish(serviceConfig.notificationARN, "world", "hello")
    }

    Test
    fun testComputeTargetUnit() {
        assertThat(underTest.computeTargetUnit(5, 2.0, 0, 7)).isEqualTo(7)
        assertThat(underTest.computeTargetUnit(5, 1.0, 0, 6)).isEqualTo(5)
        assertThat(underTest.computeTargetUnit(5, 0.4, 0, 6)).isEqualTo(2)
        assertThat(underTest.computeTargetUnit(5, 0.1, 0, 6)).isEqualTo(0)
        assertThat(underTest.computeTargetUnit(5, 0.1, 7, 10)).isEqualTo(7)
    }

    Test
    fun testShouldDownscale() {
        var config = IndexScalingConfig(index = DynamoIndex("test"), minRead = 1, minWrite = 1,
                                        maxRead = 100, maxWrite = 100, downscaleWaitMinutes = 300,
                                        enableDownscale = true)
        var description = DynamoIndexDescription("ACTIVE", DateTime.now(), DateTime.now(),
                                                 DateTime.now(), 5, 5)

        assertThat(underTest.shouldDownscale(config, description)).isFalse()

        description = description.copy(lastDecrease =
                                       DateTime(description.lastDecrease.minusMinutes(
                                               config.downscaleWaitMinutes + 1)))
        assertThat(underTest.shouldDownscale(config, description)).isFalse()

        description = description.copy(lastIncrease =
                                       DateTime(description.lastIncrease.minusMinutes(
                                               config.downscaleWaitMinutes + 1)))
        assertThat(underTest.shouldDownscale(config, description)).isTrue()

        config = config.copy(enableDownscale = false)
        assertThat(underTest.shouldDownscale(config, description)).isFalse()
        config = config.copy(enableDownscale = true, maxRead = 3, maxWrite = 3)
        assertThat(underTest.shouldDownscale(config, description)).isFalse()
    }

    Test
    fun testShouldUpscale() {
        var config = IndexScalingConfig(index = DynamoIndex("test"), minRead = 1, minWrite = 1,
                                        maxRead = 100, maxWrite = 100, downscaleWaitMinutes = 300,
                                        enableUpscale = true)
        var description = DynamoIndexDescription("ACTIVE", DateTime.now(), DateTime.now(),
                                                 DateTime.now(), 5, 5)

        assertThat(underTest.shouldUpscale(config, description)).isTrue()
        config = config.copy(enableUpscale = false)
        assertThat(underTest.shouldUpscale(config, description)).isFalse()
        config = config.copy(enableUpscale = true, maxRead = 3, maxWrite = 3)
        assertThat(underTest.shouldDownscale(config, description)).isFalse()
    }

    Test
    fun testIsScalingOverridden() {
        var config = IndexScalingConfig(index = DynamoIndex("test"), minRead = 1, minWrite = 1,
                                        maxRead = 100, maxWrite = 100, downscaleWaitMinutes = 300,
                                        enableUpscale = true)
        var description = DynamoIndexDescription("ACTIVE", DateTime.now(), DateTime.now(),
                                                 DateTime.now(), 5, 5)

        assertThat(underTest.isScalingOverridden(config, description)).isFalse()
        config = config.copy(maxRead = 3)
        assertThat(underTest.isScalingOverridden(config, description)).isTrue()
        config = config.copy(maxRead = 100, maxWrite = 3)
        assertThat(underTest.isScalingOverridden(config, description)).isTrue()
        config = config.copy(maxWrite = 100, minRead = 6)
        assertThat(underTest.isScalingOverridden(config, description)).isTrue()
        config = config.copy(minRead = 100, minWrite = 6)
        assertThat(underTest.isScalingOverridden(config, description)).isTrue()
    }

    Test
    fun testCreateUpdateRequest() {
        val targetReads = 3L
        val targetWrites = 3L
        var index = DynamoIndex("test")

        var request = underTest.createUpdateRequest(index, targetReads, targetWrites)
        assertThat(request.getTableName()).isEqualTo(index.tableName)
        assertThat(request.getProvisionedThroughput()).isEqualTo(
                ProvisionedThroughput(targetReads, targetWrites))
        assertThat(request.getGlobalSecondaryIndexUpdates()).isNull()

        index = index.copy(gsiName = "index")
        request = underTest.createUpdateRequest(index, targetReads, targetWrites)
        assertThat(request.getTableName()).isEqualTo(index.tableName)
        assertThat(request.getProvisionedThroughput()).isNull()
        assertThat(request.getGlobalSecondaryIndexUpdates()).hasSize(1)
        val update = request.getGlobalSecondaryIndexUpdates().get(0)
        assertThat(update.getUpdate().getProvisionedThroughput()).isEqualTo(
                ProvisionedThroughput(targetReads, targetWrites))
        assertThat(update.getUpdate().getIndexName()).isEqualTo(index.gsiName)
    }

    Test
    fun testCheckAndUpdateProvisioning() {
        val throughput = ProvisionedThroughputDescription()
                .withReadCapacityUnits(7L)
                .withWriteCapacityUnits(8L)
                .withLastDecreaseDateTime(DateTime.now().toDate())
                .withLastIncreaseDateTime(DateTime.now().toDate())
        val tableDescription = TableDescription()
                .withTableStatus("ACTIVE")
                .withProvisionedThroughput(throughput)
        mWhen(mockDynamoClient.describeTable(any<String>())).thenReturn(
                DescribeTableResult().withTable(tableDescription))

        val metrics = GetMetricStatisticsResult().withDatapoints(Datapoint().withSum(3000.0))
        mWhen(mockCloudWatchClient.getMetricStatistics(any())).thenReturn(metrics)

        val config = IndexScalingConfig(DynamoIndex("test"))
        underTest.checkAndUpdateProvisioning(config)

        verify(mockDynamoClient).updateTable(any())
    }

    Test
    fun testUpdateProvisionedThroughput() {
        var config = IndexScalingConfig(DynamoIndex("test"))
        val targetReads = 7L
        val targetWrites = 8L
        val consumedReads = 5.0
        val consumedWrites = 6.0
        val description = DynamoIndexDescription("ACTIVE", DateTime.now(), DateTime.now(),
                                                 DateTime.now(), 5L, 6L)

        underTest.updateProvisionedThroughput(config, description, targetReads, targetWrites,
                                              consumedReads, consumedWrites)
        verify(mockDynamoClient).updateTable(any())

        config = config.copy(maxWrite = 5)
        underTest.updateProvisionedThroughput(config, description, targetReads, targetWrites,
                                              consumedReads, consumedWrites)
        verify(mockSNSClient).publish(any(), any(), any())
    }

    Test
    fun testStartService() {
        underTest.start()
        assertThat(underTest.isShutDown()).isTrue()
        underTest.stop()
        assertThat(underTest.isShutDown()).isFalse()
    }
}