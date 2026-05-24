package com.example.typespec.workflow

internal sealed interface TypeSpecCliJobResult {
    data object AbortedByUser : TypeSpecCliJobResult

    data object Cancelled : TypeSpecCliJobResult

    data object CliUnavailable : TypeSpecCliJobResult

    /** Failure UI was already shown by the job; skip generic workflow notifications. */
    data object FailureNotified : TypeSpecCliJobResult

    data class Finished(val exitCode: Int) : TypeSpecCliJobResult
}

internal sealed interface TypeSpecCliProcessOutcome {
    data object Cancelled : TypeSpecCliProcessOutcome

    data class Exited(val exitCode: Int) : TypeSpecCliProcessOutcome

    data class FailedToStart(val message: String) : TypeSpecCliProcessOutcome
}

internal fun TypeSpecCliProcessOutcome.toJobResult(): TypeSpecCliJobResult =
    when (this) {
        TypeSpecCliProcessOutcome.Cancelled -> TypeSpecCliJobResult.Cancelled
        is TypeSpecCliProcessOutcome.Exited -> TypeSpecCliJobResult.Finished(exitCode)
        is TypeSpecCliProcessOutcome.FailedToStart -> TypeSpecCliJobResult.Finished(-1)
    }
