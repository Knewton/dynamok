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

/**
 * Provides utility methods to query information from AWS SNS
 *
 * @param clientFactory The AWSClientFactory to create the AmazonSNSConnection used to talk
 *                      to AmazonSNS
 */
public open class SNSConnection(clientFactory: AWSClientFactory) {

    val client = clientFactory.createSNSClient()

    /**
     * Publishes a notification to a given SNS topic.  The SNS message will have the given
     * subject and description.  If the subject is larger than the maximum AWS allowed length (100),
     * it will be truncated and prepended to the body.
     *
     * @param arn The ARN of the topic to publish to
     * @param subject The subject of the message to send
     * @param body The body of the message to send
     * @throws AmazonClientException If the client request failed
     */
    public open fun postNotification(arn: String, subject: String, body: String) {
        var editedSubject = subject
        var editedBody = body
        if (subject.length() > MAX_SUBJECT_LENGTH) {
            editedSubject = subject.substring(0..MAX_SUBJECT_LENGTH - 1)
            editedBody = "$subject\n\n$body"
        }
        client.publish(arn, editedBody, editedSubject)
    }

    companion object {
        private val MAX_SUBJECT_LENGTH = 100
    }
}