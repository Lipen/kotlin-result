@file:Suppress("unused")

package com.github.lipen.result

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Result<out V, out E : Exception> {
    abstract fun get(): V

    data class Success<out V> internal constructor(val value: V) : Result<V, Nothing>() {
        override fun get(): V = value
        override fun toString(): String = "Success($value)"
    }

    data class Failure<out E : Exception> internal constructor(val error: E) : Result<Nothing, E>() {
        override fun get(): Nothing = throw error
        override fun toString(): String = "Failure($error)"
    }

    companion object Factory {
        fun <V> success(value: V): Result<V, Nothing> = Success(value)
        fun <E : Exception> failure(error: E): Result<Nothing, E> = Failure(error)

        inline fun <V : Any> of(
            value: V?,
            onNull: () -> Exception = { IllegalArgumentException("null passed to Result::of") }
        ): Result<V, Exception> =
            value?.let { success(it) } ?: failure(onNull())
    }
}

inline fun <R, reified E : Exception> runCatching(block: () -> R): Result<R, E> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        Result.success(block())
    } catch (ex: Exception) {
        (ex as? E)?.let { Result.failure(it) }
            ?: throw IllegalStateException("runCatching caught an unexpected exception", ex)
    }
}

inline fun <T, R, reified E : Exception> T.runCatching(block: T.() -> R): Result<R, E> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        Result.success(block())
    } catch (ex: Exception) {
        (ex as? E)?.let { Result.failure(it) }
            ?: throw IllegalStateException("runCatching caught an unexpected exception", ex)
    }
}

inline fun <V, E : Exception, R> Result<V, E>.fold(onSuccess: (V) -> R, onFailure: (E) -> R): R {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> onSuccess(value)
        is Result.Failure -> onFailure(error)
    }
}

inline fun <V, E : Exception> Result<V, E>.onSuccess(block: (V) -> Unit): Result<V, E> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    fold(block, {})
    return this
}

inline fun <V, E : Exception> Result<V, E>.onFailure(block: (E) -> Unit): Result<V, E> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    fold({}, block)
    return this
}

inline fun <V : R, E : Exception, R> Result<V, E>.getOrElse(onFailure: (E) -> R): R {
    contract {
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return fold({ it }, onFailure)
}

fun <V : R, E : Exception, R> Result<V, E>.getOrDefault(default: R): R =
    fold({ it }, { default })

inline fun <V, E : Exception, R> Result<V, E>.flatMap(transform: (V) -> Result<R, E>): Result<R, E> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> transform(value)
        is Result.Failure -> this
    }
}

inline fun <V, E : Exception, R> Result<V, E>.map(transform: (V) -> R): Result<R, E> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return flatMap { value -> Result.success(transform(value)) }
}

inline fun <V, E : F, R, reified F : Exception> Result<V, E>.mapCatching(transform: (V) -> R): Result<R, F> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> runCatching { transform(value) }
        is Result.Failure -> this
    }
}

inline fun <V, E : Exception, F : Exception> Result<V, E>.flatMapError(transform: (E) -> Result<V, F>): Result<V, F> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> transform(error)
    }
}

inline fun <V, E : F, F : Exception> Result<V, E>.mapError(transform: (E) -> F): Result<V, F> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return flatMapError { error -> Result.failure(transform(error)) }
}

inline fun <V : R, E : Exception, R> Result<V, E>.recover(transform: (E) -> R): Result<R, Nothing> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> Result.success(transform(error))
    }
}

inline fun <V : R, E : Exception, R, reified F : Exception> Result<V, E>.recoverCatching(transform: (E) -> R): Result<R, F> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Result.Success -> this
        is Result.Failure -> runCatching { transform(error) }
    }
}
