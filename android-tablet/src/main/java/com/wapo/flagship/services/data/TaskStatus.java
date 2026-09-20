package com.wapo.flagship.services.data;

public enum TaskStatus {
    ///
    /// initial state
    NotInitialized,
    ///
    /// Added into the processing queue
    Pending,
    Running,
    Complete,
    Canceled,
    Error
}
