package com.maksing.reqsdemo;

/**
 * Created by maksing on 6/6/15.
 */
interface Callback<T, S> {
    void onSuccess(T response);
    void onFailure(S response);
}
