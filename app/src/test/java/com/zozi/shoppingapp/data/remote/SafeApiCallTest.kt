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
    fun `wraps a successful call`() = runTest {
        val result = safeApiCall(dispatcher) { "payload" }

        assertEquals(Outcome.Success("payload"), result)
    }

    @Test
    fun `converts thrown transport failures into domain errors`() = runTest {
        val result = safeApiCall<String>(dispatcher) { throw UnknownHostException() }

        assertEquals(Outcome.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `lets cancellation propagate instead of reporting it as an error`() = runTest {
        val started = CompletableDeferred<Unit>()

        val job = launch {
            safeApiCall<String>(dispatcher) {
                started.complete(Unit)
                throw CancellationException("cancelled upstream")
            }
        }
        started.await()
        job.join()

        assertTrue(job.isCancelled)
    }
}
