package com.desafiomusical.app.domain.model

enum class AnswerType {
    TITLE,
    ARTIST
}

enum class AttemptType {
    MAIN_ANSWER,
    STEAL_ANSWER,
    HINT_REQUEST
}

enum class AttemptOutcome {
    CORRECT_TITLE,
    CORRECT_ARTIST,
    CORRECT_BOTH,
    CORRECT_WORK,
    WRONG
}
