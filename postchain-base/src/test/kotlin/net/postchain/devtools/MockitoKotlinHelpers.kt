// Copyright (c) 2020 ChromaWay AB. See README for license information.

package net.postchain.devtools

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import org.mockito.ArgumentCaptor

// FYI: Kotlin/MockitoKotlin can't infer return type correctly here w/o explicit declaration
inline fun <reified T1 : Any, reified T2 : Any> argumentCaptor2():
        Pair<KArgumentCaptor<T1>, KArgumentCaptor<T2>> {

    return Pair(
            KArgumentCaptor(ArgumentCaptor.forClass(T1::class.java), T1::class),
            KArgumentCaptor(ArgumentCaptor.forClass(T2::class.java), T2::class))
}

inline fun <reified T1 : Any, reified T2 : Any, reified T3 : Any> argumentCaptor3():
        Triple<KArgumentCaptor<T1>, KArgumentCaptor<T2>, KArgumentCaptor<T3>> {

    return Triple(
            KArgumentCaptor(ArgumentCaptor.forClass(T1::class.java), T1::class),
            KArgumentCaptor(ArgumentCaptor.forClass(T2::class.java), T2::class),
            KArgumentCaptor(ArgumentCaptor.forClass(T3::class.java), T3::class))
}
