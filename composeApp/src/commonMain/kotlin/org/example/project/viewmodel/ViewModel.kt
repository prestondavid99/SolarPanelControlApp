package org.example.project.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob

open class ViewModel {
    private val job = SupervisorJob()
    protected val viewModelScope = CoroutineScope(Dispatchers.IO)
}