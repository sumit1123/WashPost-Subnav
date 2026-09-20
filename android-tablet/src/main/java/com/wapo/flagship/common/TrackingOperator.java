package com.wapo.flagship.common;

import androidx.annotation.NonNull;

import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Tracks the {@link Observable} status (started or finished) without any transformations
 */
public class TrackingOperator<T> implements Observable.Operator<T, T> {

    @NonNull
    private final String tag;
    private final TrackListener trackListener;

    public TrackingOperator(@NonNull String tag, @NonNull TrackListener trackListener) {
        this.tag = tag;
        this.trackListener = trackListener;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> s) {
        final TrackSubscriber<T> parent = new TrackSubscriber<>(s, tag, trackListener);
        s.add(parent);
        parent.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                parent.onUnsubscribe();
            }
        }));
        return parent;
    }

    public interface TrackListener {
        /**
         * This method is invoked when the Subscriber and Observable have been connected but the Observable has
         * not yet begun to emit items or send notifications to the Subscriber
         * <p/>
         * Invokes of this method are not thread safe and are not synchronized
         */
        void onStart(String tag);

        /**
         * This method is invoked when Observable is completed or finished with an error
         * <p/>
         * Invokes of this method are not thread safe and are not synchronized
         */
        void onFinish(String tag);
    }

    static final class TrackSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super T> actual;
        private final String tag;
        final TrackListener trackListener;
        volatile boolean done;

        TrackSubscriber(Subscriber<? super T> actual, String tag, TrackListener trackListener) {
            this.actual = actual;
            this.tag = tag;
            this.trackListener = trackListener;
        }

        @Override
        public void onCompleted() {
            if (!done) {
                actual.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!done) {
                actual.onError(e);
                done = true;
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void setProducer(Producer p) {
            actual.setProducer(p);
        }

        @Override
        public void onStart() {
            trackListener.onStart(tag);
        }

        void onUnsubscribe() {
            trackListener.onFinish(tag);
        }
    }
}
