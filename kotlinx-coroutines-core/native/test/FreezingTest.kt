/*
 * Copyright 2016-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreezingTest : TestBase() {
    private val dispatcher = newSingleThreadContext("WorkerCoroutineDispatcherTest")

    @Test
    fun testFreezeWithContextOther() = runTest {
        // create a mutable object referenced by this lambda
        val mutable = mutableListOf<Int>()
        // run a child coroutine in another thread
        val result = withContext(Dispatchers.Default) { "OK" }
        assertEquals("OK", result)
        // ensure that objects referenced by this lambda were not frozen
        assertFalse(mutable.isFrozen)
        mutable.add(42) // just to be 100% sure
    }

    @Test
    fun testNoFreezeLaunchSame() = runTest {
        // create a mutable object referenced by this lambda
        val mutable1 = mutableListOf<Int>()
        // this one will get captured into the other thread's lambda
        val mutable2 = mutableListOf<Int>()
        val job = launch { // launch into the same context --> should not freeze
            assertEquals(mutable1.isFrozen, false)
            assertEquals(mutable2.isFrozen, false)
            val result = withContext(Dispatchers.Default) {
                assertEquals(mutable2.isFrozen, true) // was frozen now
                "OK"
            }
            assertEquals("OK", result)
            assertEquals(mutable1.isFrozen, false)
        }
        job.join()
        assertEquals(mutable1.isFrozen, false)
        mutable1.add(42) // just to be 100% sure
    }

    @Test
    fun testFrozenParentJob() {
        val parent = Job()
        parent.freeze()
        val job = Job(parent)
        assertTrue(job.isActive)
        parent.cancel()
        assertTrue(job.isCancelled)
    }

    @Test
    fun testFrozenCollector() = runBlocking<Unit>() {
        flow {
            emit(0)
        }.flowOn(Dispatchers.Default)
            .collect {
                assert(this.isFrozen == true)
            }
    }
}
