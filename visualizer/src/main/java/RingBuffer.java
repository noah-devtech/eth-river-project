import processing.core.PVector;

public class RingBuffer {
    private final PVector[] buffer;
    private final int capacity;
    private int head;
    private int count = 0;


    public RingBuffer(int capacity) {
        this.buffer = new PVector[capacity];
        this.capacity = capacity;
        for (int i = 0; i < capacity; i++) {
            buffer[i] = new PVector();
        }
        this.head = 0;
        this.count = 0;
    }

    public void add(PVector v) {
        buffer[head].set(v);
        head = (head + 1) % capacity;
        if (count < capacity) {
            count++;
        }
    }

    public PVector get(int i) {
        if ((i < 0) || (capacity < i)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int desireIndex = (head - i - 1 + capacity) % capacity;
        return buffer[desireIndex];
    }

    public int size() {
        return count;
    }

    public void clear() {
        head = 0;
    }

}