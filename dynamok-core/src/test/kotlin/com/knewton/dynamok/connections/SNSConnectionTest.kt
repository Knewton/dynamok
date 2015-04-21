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

import com.amazonaws.services.sns.AmazonSNSClient
import com.knewton.dynamok.any
import com.knewton.dynamok.eq
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as mWhen

class SNSConnectionTest {

    val mockClientFactory = mock(javaClass<AWSClientFactory>())
    val mockClient = mock(javaClass<AmazonSNSClient>())

    Test
    fun testPostNotification() {
        mWhen(mockClientFactory.createSNSClient()).thenReturn(mockClient)
        val underTest = SNSConnection(mockClientFactory)
        underTest.postNotification("arn", "subject", "body")
        verify(mockClient).publish("arn", "body", "subject")
    }

    Test
    fun testPostNotificationLongSubject() {
        mWhen(mockClientFactory.createSNSClient()).thenReturn(mockClient)
        val underTest = SNSConnection(mockClientFactory)
        val longSubject = RandomStringUtils.randomAlphabetic(101)

        underTest.postNotification("arn", longSubject, "body")
        verify(mockClient).publish(eq("arn"), any(), eq(longSubject.substring(0..99)))
    }
}