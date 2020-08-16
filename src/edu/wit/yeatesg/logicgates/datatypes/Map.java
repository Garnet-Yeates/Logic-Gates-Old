package edu.wit.yeatesg.logicgates.datatypes;

import java.util.ArrayList;
import java.util.Comparator;

public class Map<K, V> {

    private ArrayList<MapEntry<K, V>> entries = new ArrayList<>();

    public Map(Map<K, V> other) {
        entries = new ArrayList<>(other.entries);
    }

    public Map() { }

    public void clear() {
        entries.clear();
    }

    public void addAll(Map<K, V> other) {
        entries.addAll(other.entries);
    }

    public void sortByValue(Comparator<? super MapEntry<K, V>> comp) {
        entries.sort(comp);
    }

    public static class MapEntry<K, V> {
        private K key;
        private V value;

        private MapEntry(K key, V value) {
            if (key == null)
                throw new NullPointerException("Null key");
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }

    public void put(K key, V value) {
        int indexOfKey = indexOf(key);
        if (indexOfKey != -1)
            entries.get(indexOfKey).value = value;
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

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public ArrayList<MapEntry<K, V>> getEntries() {
        return entries;
    }

    public ArrayList<K> keySet() {
        ArrayList<K> list = new ArrayList<>();
        entries.forEach(kvMapEntry -> list.add(kvMapEntry.key));
        return list;
    }

    public ArrayList<V> values() {
        ArrayList<V> list = new ArrayList<>();
        entries.forEach(kvMapEntry -> list.add(kvMapEntry.value));
        return list;
    }
}
