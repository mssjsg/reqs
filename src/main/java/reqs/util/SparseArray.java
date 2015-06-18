package reqs.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maksing on 14/6/15.
 */
public class SparseArray<T> {
    private List<T> objects = new ArrayList<>();
    private List<Integer> keys = new ArrayList<>();

    public int size() {
        return objects.size();
    }

    private int indexOfKey(int key) {
        int i;
        for (i = 0; i < keys.size(); i++) {
            if (keys.get(i) == key) {
                return i;
            }
        }
        return -1;
    }

    public T get(int key) {
        int i = indexOfKey(key);
        if (i != -1) {
            return objects.get(i);
        }
        return null;
    }

    public int keyAt(int position) {
        if (keys.size() > position) {
            return keys.get(position);
        }
        return 0;
    }

    synchronized public void put(int key, T object) {
        objects.add(object);
        keys.add(key);
    }

    synchronized public void remove(int key) {
        int i = indexOfKey(key);
        if (i != -1) {
            objects.remove(i);
            keys.remove(i);
        }

    }

    public List<T> getObjects() {
        return objects;
    }
}