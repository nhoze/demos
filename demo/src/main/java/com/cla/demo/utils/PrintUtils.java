package com.cla.demo.utils;

public final class PrintUtils {

    public static void threadPrinting(String message) {
        System.out.println(String.format("%s - %s", Thread.currentThread().getName(), message));
    }
}
