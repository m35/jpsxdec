/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.player;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ObjectPool<T> {

    private int _iBalance = 0;

    private final Queue<T> objects;

    public ObjectPool() {
        this.objects = new ConcurrentLinkedQueue<T>();
    }

    public ObjectPool(Collection<? extends T> objects) {
        this.objects = new ConcurrentLinkedQueue<T>(objects);
    }

    protected abstract T createExpensiveObject();

    public T borrow() {
        T t;
        if ((t = objects.poll()) == null) {
            t = createExpensiveObject();
        }
        _iBalance++;
        return t;
    }

    public void giveBack(T object) {
        this.objects.offer(object);   // no point to wait for free space, just return
        _iBalance--;
        if (_iBalance == 0) {
            System.err.println("Object pool balanced.");
        }
    }
}
