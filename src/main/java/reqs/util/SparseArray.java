package reqs.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by maksing on 14/6/15.
 */
public class SparseArray<T> {
//    private List<T> objects = new ArrayList<>();
//    private List<Integer> keys = new ArrayList<>();
    private List<Item<T>> items = new ArrayList<>();

    public int size() {
        return items.size();
    }

    private int indexOfKey(int key) {
        int i;
        for (i = 0; i < items.size(); i++) {
            if (items.get(i).key == key) {
                return i;
            }
        }
        return -1;
    }

    public T get(int key) {
        int i = indexOfKey(key);
        if (i != -1) {
            return items.get(i).object;
        }
        return null;
    }

    public int keyAt(int position) {
        if (items.size() > position) {
            return items.get(position).key;
        }
        return 0;
    }

    synchronized public void put(int key, T object) {
        items.add(new Item<T>(key, object));
    }

    synchronized public void remove(int key) {
        int i = indexOfKey(key);
        if (i != -1) {
            items.remove(i);
        }

    }

    public List<T> getObjects() {
        List<T> objects = new ArrayList<>();
        for (Item<T> item : items) {
            objects.add(item.object);
        }
        return objects;
    }

    private static class Item<E> {
        private final int key;
        private final E object;

        private Item(int key, E object) {
            this.key = key;
            this.object = object;
        }
    }

    public SparseArray<T> getSortedItems() {
        List<Item<T>> sortedItems = new ArrayList<>(items);
        Collections.sort(sortedItems, new KeyComparator());
        SparseArray<T> sparseArray = new SparseArray<>();
        sparseArray.items = sortedItems;
        return sparseArray;
    }

    public static class KeyComparator implements Comparator<Item<?>> {

        @Override
        public int compare(Item<?> o1, Item<?> o2) {
            return o1.key - o2.key;
        }
    }
}