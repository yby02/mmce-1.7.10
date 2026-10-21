package github.kasuminova.mmce.common.util.concurrent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Queues {

    /**
     * 1.7.10 移植：1.7.10 自带的 netty 不含 jctools，统一使用 JDK 的无锁队列。
     */
    public static <E> Queue<E> createConcurrentQueue() {
        return new ConcurrentLinkedQueue<>();
    }

}
