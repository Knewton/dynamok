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

/**
 * Represents a DynamoDb table index.  This is either a primary index (if the provided global
 * secondary index is null or empty) or a global secondary index (if the provided global secondary
 * index is not null or empty).
 *
 * @param tableName The table name of the primary or global secondary index
 * @param gsiName The global secondary index name corresponding to the table's index (may be null)
 */
public data class DynamoIndex(// The table name
                              val tableName: String,
                              // The optional global secondary index name
                              val gsiName: String? = null)