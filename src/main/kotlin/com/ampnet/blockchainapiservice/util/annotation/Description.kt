package com.ampnet.blockchainapiservice.util.annotation

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val value: String)
