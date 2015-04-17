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

import org.mockito.Matchers
import sun.reflect.ReflectionFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * This class file provides Kotlin compatible matchers for Mockito
 */
public object KMockito

/**
 * Returns an instance of the specified class or interface, by either creating a proxy or
 * no-args serialization constructor and instantiating it.
 *
 * @param clazz The class type to create
 * @return An instance of the given class type
 */
public fun <T> getInstance(clazz: Class<T>): T {
    if (clazz.isInterface()) {
        return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), array(clazz),
                                                 InvocationHandler { any, method, args -> }))
    } else {
        val constructor = ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(clazz,
                                                javaClass<Any>().getDeclaredConstructor())
        return clazz.cast(constructor.newInstance())
    }
}

/**
 * Similar to Mockito's eq() matcher.
 *
 * @param value The expected value of the object to match.
 * @return The object value
 */
public fun <T> eq(value: T): T = Matchers.eq(value) ?: value

/**
 * Similar to Mockito's any(class) matcher
 *
 * @param clazz The class type to match
 * @return An instance of the class
 */
public fun <T> any(clazz: Class<T>): T = Matchers.any(clazz) ?: getInstance(clazz)

/**
 * Similar to Mockito's any() matcher
 *
 * @return An instance of the class
 */
public inline fun <reified T> any(): T = Matchers.any() ?: getInstance(javaClass<T>())

/**
 * Similar to Mockito's anyObject matcher
 *
 * @return An instance of the class
 */
public inline fun <reified T> anyObject(): T = any()