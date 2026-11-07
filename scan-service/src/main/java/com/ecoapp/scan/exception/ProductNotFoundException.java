package com.ecoapp.scan.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String barcode) {
        super("Product not found for barcode: " + barcode);
    }
}
