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

package com.knewton.dynamok.data

import org.joda.time.DateTime

/**
 * Container data class holding information retrieved about a table's primary or global secondary
 * index.  Used by DynamoConnection when retrieving information.
 */
public open data class DynamoIndexDescription(// The current table or index status (ex: ACTIVE)
                                              val status: String,
                                              // The time the table was created
                                              val created: DateTime,
                                              // The last time the index provisioning was decreased
                                              val lastDecrease: DateTime,
                                              // The last time the index provisioning was increased
                                              val lastIncrease: DateTime,
                                              // The provisioned read capacity units
                                              val readCapacity: Long,
                                              // The provisioned write capacity units
                                              val writeCapacity: Long)