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

package com.knewton.dynamok.connections

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.model.Datapoint
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult
import com.knewton.dynamok.any
import com.knewton.dynamok.data.DynamoIndex
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as mWhen

class CloudWatchConnectionTest {

    val mockClientFactory = mock(javaClass<AWSClientFactory>())
    val mockClient = mock(javaClass<AmazonCloudWatchClient>())

    Test
    fun testGetTableMetricAverage() {
        val sum = 42.0
        val metricsResult = GetMetricStatisticsResult()
                .withDatapoints(Datapoint().withSum(sum))
        mWhen(mockClient.getMetricStatistics(any())).thenReturn(metricsResult)
        mWhen(mockClientFactory.createCloudWatchClient()).thenReturn(mockClient)
        val underTest = CloudWatchConnection(mockClientFactory)

        assertThat(underTest.getTableMetricAverage(DynamoIndex("test"), "metric")).isEqualTo(
                sum / 300.0)
    }

    Test
    fun testGetTableMetricAverageNullData() {
        val metricsResult = GetMetricStatisticsResult()
        mWhen(mockClient.getMetricStatistics(any())).thenReturn(metricsResult)
        mWhen(mockClientFactory.createCloudWatchClient()).thenReturn(mockClient)
        val underTest = CloudWatchConnection(mockClientFactory)

        assertThat(underTest.getTableMetricAverage(DynamoIndex("test"), "metric")).isEqualTo(0.0)
    }
}