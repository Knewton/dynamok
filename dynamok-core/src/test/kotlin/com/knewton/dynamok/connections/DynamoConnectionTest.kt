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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.*
import com.knewton.dynamok.any
import com.knewton.dynamok.data.DynamoIndex
import com.knewton.dynamok.data.DynamoIndexDescription
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.DateTime
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Date
import org.mockito.Mockito.`when` as mWhen

class DynamoConnectionTest {

    val mockClientFactory = mock(javaClass<AWSClientFactory>())
    val mockClient = mock(javaClass<AmazonDynamoDBClient>())

    Test
    fun testGetDescription() {
        val throughput = ProvisionedThroughputDescription()
                .withReadCapacityUnits(24L)
                .withWriteCapacityUnits(24L)
                .withLastDecreaseDateTime(Date())
                .withLastIncreaseDateTime(Date())
        val description = TableDescription()
                .withCreationDateTime(Date())
                .withProvisionedThroughput(throughput)
                .withTableStatus(TableStatus.ACTIVE)
        mWhen(mockClient.describeTable(any<String>())).thenReturn(
                DescribeTableResult().withTable(description))
        mWhen(mockClientFactory.createDynamoClient()).thenReturn(mockClient)
        val underTest = DynamoConnection(mockClientFactory)

        val expected = DynamoIndexDescription(description.getTableStatus(),
                                              DateTime(description.getCreationDateTime()),
                                              DateTime(throughput.getLastDecreaseDateTime()),
                                              DateTime(throughput.getLastIncreaseDateTime()),
                                              throughput.getReadCapacityUnits(),
                                              throughput.getWriteCapacityUnits())
        assertThat(underTest.getDescription(DynamoIndex("test"))).isEqualTo(expected)
    }

    Test
    fun testGetGSIDescription() {
        val throughput = ProvisionedThroughputDescription()
                .withReadCapacityUnits(24L)
                .withWriteCapacityUnits(24L)
                .withLastDecreaseDateTime(Date())
                .withLastIncreaseDateTime(Date())
        val gsiDescription = GlobalSecondaryIndexDescription()
                .withIndexName("gsi")
                .withProvisionedThroughput(throughput)
                .withIndexStatus(IndexStatus.ACTIVE)
        val description = TableDescription()
                .withCreationDateTime(Date())
                .withGlobalSecondaryIndexes(gsiDescription)
        mWhen(mockClient.describeTable(any<String>())).thenReturn(
                DescribeTableResult().withTable(description))
        mWhen(mockClientFactory.createDynamoClient()).thenReturn(mockClient)
        val underTest = DynamoConnection(mockClientFactory)

        val expected = DynamoIndexDescription(gsiDescription.getIndexStatus(),
                                              DateTime(description.getCreationDateTime()),
                                              DateTime(throughput.getLastDecreaseDateTime()),
                                              DateTime(throughput.getLastIncreaseDateTime()),
                                              throughput.getReadCapacityUnits(),
                                              throughput.getWriteCapacityUnits())
        assertThat(underTest.getDescription(DynamoIndex("test", "gsi"))).isEqualTo(expected)
    }
}