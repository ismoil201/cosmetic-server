package com.example.backend.entity;

public enum EventType {
    IMPRESSION,      // home’da ko‘rindi
    CLICK,           // card bosildi
    VIEW,            // detail ochildi
    FAVORITE_ADD,
    FAVORITE_REMOVE,
    ADD_TO_CART,
    REMOVE_FROM_CART,
    PURCHASE,
    SEARCH,          // query yuborildi
    SEARCH_CLICK     // search natijasidan bosildi
}
