package com.zozi.shoppingapp.data.remote

import com.zozi.shoppingapp.domain.error.AppError
import com.zozi.shoppingapp.domain.result.Outcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class SafeApiCallTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `given a call that returns when wrapping it then the value comes back as Success`() = runTest {
        // Given a block that completes normally

        // When
        val result = safeApiCall(dispatcher) { "payload" }

        // Then
        assertEquals(Outcome.Success("payload"), result)
    }

    @Test
    fun `given a call that throws a transport failure when wrapping it then it becomes a Failure`() = runTest {
        // Given a block that fails the way the network does

        // When
        val result = safeApiCall<String>(dispatcher) { throw UnknownHostException() }

        // Then
        assertEquals(Outcome.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `given a cancelled call when wrapping it then cancellation propagates instead of becoming an error`() = runTest {
        // Given a block that is cancelled rather than failing
        val started = CompletableDeferred<Unit>()

        // When
        val job = launch {
            safeApiCall<String>(dispatcher) {
                started.complete(Unit)
                throw CancellationException("cancelled upstream")
            }
        }
        started.await()
        job.join()

        // Then the coroutine is cancelled, not reported as a failed request
        assertTrue(job.isCancelled)
    }
}
