package org.example.project.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

/**
 * ViewModel: A base class for ViewModels in the MVVM (Model-View-ViewModel) architecture.
 * This class provides a structured way to manage coroutines and background tasks for ViewModels.
 */
open class ViewModel {
    /**
     * SupervisorJob for managing coroutines.
     * A SupervisorJob allows child coroutines to fail independently without affecting other children.
     */
    private val job = SupervisorJob()

    /**
     * CoroutineScope for the ViewModel.
     * This scope is used to launch coroutines that are tied to the ViewModel's lifecycle.
     * It uses Dispatchers.IO for background operations, suitable for network and database tasks.
     */
    protected val viewModelScope = CoroutineScope(Dispatchers.IO)
}