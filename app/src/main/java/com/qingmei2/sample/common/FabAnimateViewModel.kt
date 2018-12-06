package com.qingmei2.sample.common

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import arrow.core.left
import arrow.core.right
import com.qingmei2.rhine.base.viewmodel.BaseViewModel
import com.qingmei2.rhine.ext.lifecycle.bindLifecycle
import com.qingmei2.sample.entity.Errors
import com.qingmei2.sample.http.RxSchedulers
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject

/**
 * Common ViewModel, see:
 * [com.qingmei2.sample.ui.main.home.HomeFragment]
 * [com.qingmei2.sample.ui.main.repos.ReposFragment]
 */
@SuppressLint("CheckResult")
class FabAnimateViewModel : BaseViewModel() {

    val visibleState: MutableLiveData<Boolean> = MutableLiveData()

    val stateChangesConsumer: (Int) -> Unit = {
        scrollStateSubject.onNext(it)
    }

    private val scrollStateSubject: PublishSubject<Int> = PublishSubject.create()

    override fun onCreate(lifecycleOwner: LifecycleOwner) {
        super.onCreate(lifecycleOwner)
        scrollStateSubject
                .map { it == RecyclerView.SCROLL_STATE_IDLE }
                .compose { upstream ->
                    upstream.zipWith(upstream.startWith(true)) { last, current ->
                        when (last == current) {
                            true -> Errors.EmptyInputError.left()
                            false -> current.right()
                        }
                    }
                }
                .flatMap { changed ->
                    changed.fold({
                        Observable.empty<Boolean>()
                    }, {
                        Observable.just(it)
                    })
                }
                .observeOn(RxSchedulers.ui)
                .bindLifecycle(this)
                .subscribe {
                    applyState(visible = it)
                }
    }

    private fun applyState(visible: Boolean) {
        visibleState.postValue(visible)
    }
}