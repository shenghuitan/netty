package io.netty.util.romtn;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author roman
 */
public class DefaultPromiseTest {

    private static final AtomicReferenceFieldUpdater<DefaultPromiseTest, Object> RESULT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromiseTest.class, Object.class, "result");

    private volatile Object result;

    @Test
    public void test() {
        System.out.println("test start..");
    }

    public static void main(String[] args) {
        new DefaultPromiseTest().test();
    }

}
