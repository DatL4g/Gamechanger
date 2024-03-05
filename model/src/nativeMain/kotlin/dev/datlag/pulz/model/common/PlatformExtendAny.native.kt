package dev.datlag.pulz.model.common

import kotlin.reflect.KClass

actual val KClass<*>.name: String
    get() = this.qualifiedName?.ifBlank { null } ?: this.simpleName?.ifBlank { null } ?: this.toString()