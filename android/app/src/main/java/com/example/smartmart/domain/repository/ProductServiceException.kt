package com.example.smartmart.domain.repository

/** A service or response failure; callers never need Retrofit-specific exceptions. */
class ProductServiceException(cause: Throwable? = null) :
    Exception("Product service request failed", cause)
