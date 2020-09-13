package io.netty.example.util;

import java.util.concurrent.atomic.AtomicLong;

public class Counter {

    public int index;

    public AtomicLong received = new AtomicLong(0);
    public AtomicLong read = new AtomicLong(0);
    public AtomicLong sent = new AtomicLong(0);
    public AtomicLong succeed = new AtomicLong(0);
    public AtomicLong failed = new AtomicLong(0);
    public AtomicLong invalid = new AtomicLong(0);
    public AtomicLong length = new AtomicLong(0);


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("index=").append(index);
        sb.append(", received=").append(received);
        sb.append(", read=").append(read);
        sb.append(", sent=").append(sent);
        sb.append(", succeed=").append(succeed);
        sb.append(", failed=").append(failed);
        sb.append(", invalid=").append(invalid);
        sb.append(", length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
