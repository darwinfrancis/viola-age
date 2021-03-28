package com.darwin.viola.age

/**
 * The class AgeOptions
 *
 * @author Darwin Francis
 * @version 1.0
 * @since 28 Mar 2021
 */
class AgeOptions private constructor(builder: Builder) {

    val preValidateFace: Boolean
    val debug: Boolean

    init {
        this.preValidateFace = builder.preValidateFace
        this.debug = builder.debug
    }

    class Builder {
        var preValidateFace: Boolean = false
            private set
        var debug: Boolean = false
            private set

        fun enableFacePreValidation() = apply { this.preValidateFace = true }
        fun build() = AgeOptions(this)
    }
}