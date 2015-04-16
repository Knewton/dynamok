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

import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest
import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.knewton.dynamok.data.DynamoIndex
import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Provides utility methods to query information from AWS CloudWatch
 *
 * @param clientFactory The AWSClientFactory to create the AmazonCloudWatchClient used to talk
 *                      to CloudWatch
 */
public open class CloudWatchConnection(clientFactory: AWSClientFactory) {

    val client = clientFactory.createCloudWatchClient()

    /**
     * Retrieves the average data of a given metric on a given index (either a table's primary
     * index or global secondary index).
     *
     * @param index The primary or global secondary index
     * @param metric The AWS metric name to retrieve (ex: ConsumedReadCapacityUnits)
     * @return Returns the average of the metric or zero if no data points were retrieved
     * @throws AmazonClientException If the client request failed
     */
    public open fun getTableMetricAverage(index: DynamoIndex, metric: String): Double {
        val end = DateTime.now() - Duration.standardMinutes(
                LOOKBACK_BUFFER_MINUTES)
        val start = end - Duration.standardSeconds(PERIOD_SECONDS.toLong())

        val dimensions = arrayListOf(Dimension().withName("TableName")
                                                .withValue(index.tableName))
        if (index.gsiName.isNotEmpty()) {
            dimensions.add(Dimension().withName("GlobalSecondaryIndexName")
                                      .withValue(index.gsiName))
        }

        val request = GetMetricStatisticsRequest()
                .withPeriod(PERIOD_SECONDS)
                .withStartTime(start.toDate())
                .withEndTime(end.toDate())
                .withMetricName(metric)
                .withNamespace("AWS/DynamoDB")
                .withDimensions(dimensions)
                .withUnit(StandardUnit.Count)

        val result = client.getMetricStatistics(request)
        val datapoint = result.getDatapoints().singleOrNull()

        return ((datapoint?.getSum()) ?: 0.0) / PERIOD_SECONDS.toDouble()
    }

    /**
     * Retrieves the average consumed read units of a given table's primary index or global
     * secondary index.
     *
     * @return Returns the average consumed read units or zero if no data points were retrieved
     * @throws AmazonClientException If the client request failed
     */
    public open fun getConsumedReads(index: DynamoIndex): Double =
            getTableMetricAverage(index, "ConsumedReadCapacityUnits")

    /**
     * Retrieves the average consumed write units of a given table's primary index or global
     * secondary index.
     *
     * @return Returns the average consumed write units or zero if no data points were retrieved
     * @throws AmazonClientException If the client request failed
     */
    public open fun getConsumedWrites(index: DynamoIndex): Double =
            getTableMetricAverage(index, "ConsumedWriteCapacityUnits")

    companion object {
        private val LOOKBACK_BUFFER_MINUTES = 5L
        private val PERIOD_SECONDS = 300
    }
}