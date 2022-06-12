package edu.kit.scc.git.ggd.voxelite.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//TODO Add shrinking
public class Memory<T> implements Iterable<Memory.Entry<T>> {

    private static final int DEFAULT_SIZE = 32;

    private int[]        keys;
    private int[]        reverse;
    private T[]          values;
    private int          pointer;
    private IntArrayList free;

    public record Entry<T>(int address, T value) {}

    public Memory() {
        this(DEFAULT_SIZE);
    }

    public Memory(int initialSize) {
        keys = new int[initialSize];
        reverse = new int[initialSize];
        values = (T[]) new Object[initialSize];
        free = new IntArrayList(Util.rangeReversed(0, initialSize).toArray());
    }

    public T get(int address) {
        return values[keys[address]];
    }

    public int add(T value) {
        if(pointer == values.length) grow();
        final int address = free.popInt();

        values[pointer] = value;
        keys[address] = pointer;
        reverse[pointer] = address;
        pointer++;

        return address;
    }

    public T remove(int address) {
        final int index = keys[address];
        final T value = values[index];
        if(value == null) return null;

        //Swap and pop
        pointer--;
        values[index] = values[pointer];
        reverse[index] = reverse[pointer];
        keys[reverse[pointer]] = index;
        values[pointer] = null;
        keys[address] = -1;
        free.add(address);

        return value;
    }

    private void grow() {
        final int previous = keys.length;
        final int next = (int) (previous * 1.5f);

        keys = Arrays.copyOf(keys, next);
        reverse = Arrays.copyOf(reverse, next);
        values = Arrays.copyOf(values, next);
        free.addAll(Util.rangeReversed(previous, next).boxed().collect(Collectors.toCollection(IntArrayList::new))); //TODO Prepend
    }

    public int size() {
        return pointer;
    }

    public int allocated() {
        return values.length;
    }

    public Stream<T> streamValues() {
        return Arrays.stream(values, 0, pointer);
    }

    public Stream<Entry<T>> stream() {
        return IntStream.range(0, pointer).mapToObj(i -> new Entry<>(reverse[i], values[i]));
    }

    @NotNull
    @Override
    public Iterator<Entry<T>> iterator() {
        //TODO CME
        return new Iterator<>() {
            private int i = 0;
            private int lastRet = -1;

            @Override
            public boolean hasNext() {
                return i < pointer;
            }

            @Override
            public Entry<T> next() {
                if(i >= pointer) throw new NoSuchElementException();
                int j = i++;
                lastRet = j;
                return new Entry<>(reverse[j], values[j]);
            }

            @Override
            public void remove() {
                if(lastRet < 0) throw new IllegalStateException();
                Memory.this.remove(reverse[lastRet]);
                i--;
                lastRet = -1;
            }
        };
    }
}
