package com.example.zitounaproject.data;

/** Résultat asynchrone d'une opération Firebase. */
public interface Callback<T> {
    void onSuccess(T result);

    void onError(String message);
}
