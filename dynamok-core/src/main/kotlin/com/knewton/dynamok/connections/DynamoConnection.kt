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

import com.knewton.dynamok.data.DynamoIndex
import com.knewton.dynamok.data.DynamoIndexDescription
import org.joda.time.DateTime

/**
 * Provides utility methods to query information from AWS DynamoDB
 *
 * @param clientFactory The AWSClientFactory to create the AmazonDynamoDBClient used to talk
 *                      to DynamoDB
 */
public open class DynamoConnection(clientFactory: AWSClientFactory) {

    val client = clientFactory.createDynamoClient()

    /**
     * Retrieves a description containing information about a table's primary or global secondary
     * index.
     *
     * @param index The primary or global secondary index
     * @return The retrieved description
     * @throws AmazonClientException If the client request failed
     * @throws IllegalArgumentException If the index could not be found
     */
    public open fun getDescription(index: DynamoIndex): DynamoIndexDescription {
        val description = client.describeTable(index.tableName).getTable()
        val created = DateTime(description.getCreationDateTime())
        var status = description.getTableStatus()
        var throughput = description.getProvisionedThroughput()

        if (index.gsiName.isNotEmpty() && description.getGlobalSecondaryIndexes() != null) {
            val indexDescription = description.getGlobalSecondaryIndexes()
                                              .firstOrNull({ it.getIndexName() == index.gsiName })
            if (indexDescription == null) {
                throw IllegalArgumentException("Could not find global secondary index " +
                                               "${index.tableName}:${index.gsiName}")
            }
            throughput = indexDescription.getProvisionedThroughput()
            status = indexDescription.getIndexStatus()
        }
        return DynamoIndexDescription(status,
                                      created,
                                      DateTime(throughput.getLastDecreaseDateTime()),
                                      DateTime(throughput.getLastIncreaseDateTime()),
                                      throughput.getReadCapacityUnits(),
                                      throughput.getWriteCapacityUnits())
    }
}