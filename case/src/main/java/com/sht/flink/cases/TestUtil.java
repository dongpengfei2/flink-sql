package com.sht.flink.cases;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class TestUtil {
    public static int fib1(int n) {
        return n < 2 ? n : fib1(n - 1) + fib1(n - 2);
    }

    public static int fib2(int n) {
        int result = 0, next = 1, temp;
        for (int i = 0; i < n; i++) {
            temp = result + next;
            result = next;
            next = temp;
        }
        return result;
    }

    public static void onlySleep() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(30, 50));
    }

}
