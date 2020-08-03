package edu.wit.yeatesg.logicgates.datatypes;

import java.util.ArrayList;

public class Map<K, V> {

    private ArrayList<MapEntry<K, V>> entries = new ArrayList<>();

    private static class MapEntry<K, V> {
        private K key;
        private V value;

        private MapEntry(K key, V value) {
            if (key == null)
                throw new NullPointerException("Null key");
            this.key = key;
            this.value = value;
        }
    }

    public void put(K key, V value) {
        int indexOfKey = indexOf(key);
        if (indexOfKey != -1)
            set(indexOfKey, value);
        else
            entries.add(new MapEntry<>(key, value));
    }

    public V get(K key) {
        int indexOfKey = indexOf(key);
        return indexOfKey == -1 ? null : getEntryAt(indexOfKey).value;
    }

    public void remove(K key) {
        int indexOfKey = indexOf(key);
        if (indexOfKey != -1)
            entries.remove(indexOfKey);
    }

    private MapEntry<K, V> getEntryAt(int index) {
        return entries.get(index);
    }

    public int indexOf(K key) {
        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).key.equals(key))
                return i;
        return -1;
    }

    private void set(int index, V value) {
        entries.get(index).value = value;
    }

    private ArrayList<K> keySet() {
        ArrayList<K> list = new ArrayList<>();
        entries.forEach(kvMapEntry -> list.add(kvMapEntry.key));
        return list;
    }

    private ArrayList<V> values() {
        ArrayList<V> list = new ArrayList<>();
        entries.forEach(kvMapEntry -> list.add(kvMapEntry.value));
        return list;
    }
}
