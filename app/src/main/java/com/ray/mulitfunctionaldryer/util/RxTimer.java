package com.ray.mulitfunctionaldryer.util;


import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class RxTimer {

    private static Disposable mDisposable;

    /**
     * 每隔milliseconds毫秒后执行next操作
     *
     * @param milliseconds
     * @param next
     */
    public void interval(long milliseconds, final IRxNext next) {
        Observable.interval(milliseconds, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable disposable) {
                        mDisposable = disposable;
                    }

                    @Override
                    public void onNext(@NonNull Object number) {
                        if (next != null) {
                            next.doNext(number);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        cancel();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 取消订阅
     */
    public void cancel() {
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
            Log.e("Sys", "======BT定时器取消======");
        }
    }

    public interface IRxNext {
        void doNext(Object number);
    }


}