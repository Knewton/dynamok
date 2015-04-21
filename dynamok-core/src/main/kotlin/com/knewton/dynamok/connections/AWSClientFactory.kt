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

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.sns.AmazonSNSClient

/**
 * Provides instances of AWS clients with the given credentials and configurations.
 *
 * @param credentials AWS credentials
 * @param configuration The (optional) client configuration used to instantiate the clients with
 */
public open class AWSClientFactory(val credentials: AWSCredentials,
                                   val configuration: ClientConfiguration = ClientConfiguration()) {

    /**
     * Instantiates a DynamoDB client
     *
     * @return Returns a new DynamoDB client with credentials and configuration
     */
    public open fun createDynamoClient(): AmazonDynamoDBClient =
            AmazonDynamoDBClient(credentials, configuration)

    /**
     * Instantiates a CloudWatch client
     *
     * @return Returns a new CloudWatch client with credentials and configuration
     */
    public open fun createCloudWatchClient(): AmazonCloudWatchClient =
            AmazonCloudWatchClient(credentials, configuration)

    /**
     * Instantiates a SNS client
     *
     * @return Returns a new SNS client with credentials and configuration
     */
    public open fun createSNSClient(): AmazonSNSClient =
            AmazonSNSClient(credentials, configuration)
}