package com.automation.utils;

public final class TestDataGenerator {

    private TestDataGenerator() {

    }

    public static String generateUniqueEmail() {
        return "qa_auto_" + System.currentTimeMillis() + "@test.com";
    }

    public static String generateUniqueUsername() {
        return "qa_auto_" + System.currentTimeMillis();
    }

    public static int generateUniqueTableNumber() {
        return (int) (System.currentTimeMillis() % 9000) + 1000;
    }
}
