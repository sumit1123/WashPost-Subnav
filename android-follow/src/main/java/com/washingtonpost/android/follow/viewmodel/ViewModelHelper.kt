package com.washingtonpost.android.follow.viewmodel

import android.app.Application
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.washingtonpost.android.follow.helper.FollowManager
import kotlin.reflect.KClass

object ViewModelHelper {
    inline fun <reified T : ViewModel> getViewModel(source: Any, clazz: KClass<T>): Lazy<T> {
        return when (source) {
            is AppCompatActivity -> source.viewModels { getViewModelFactory(source, source.application, clazz) }
            is Fragment -> source.activityViewModels { getViewModelFactory(source, source.requireActivity().application, clazz) }
            else -> throw IllegalStateException("This only works for activities and fragments.")
        }
    }

    fun <T : ViewModel> getViewModelFactory(owner: SavedStateRegistryOwner, application: Application, clazz: KClass<T>): AbstractSavedStateViewModelFactory {
        return object : AbstractSavedStateViewModelFactory(owner, null) {
            override fun <U : ViewModel> create(key: String, modelClass: Class<U>, handle: SavedStateHandle): U {
                clazz.constructors.forEach { con ->
                    if (con.parameters.size == 2) {
                        @Suppress("UNCHECKED_CAST")
                        return con.call(FollowManager.getInstance(application), handle) as U
                    }
                }
                throw IllegalStateException("This only works for specific follow view models.")
            }
        }
    }
}