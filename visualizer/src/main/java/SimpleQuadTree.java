import java.util.ArrayList;
import java.util.List;

/*
 * [ORIGINAL LICENSE]
 * MIT License
 * Copyright (c) 2017 Guilherme de Cleva Farto
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * -----------------------------------------------------------------------
 * [MODIFICATION NOTICE]
 * Ported to Java and optimized for object pooling by noah-devtech, 2026.
 * - Converted from Processing (.pde) to pure Java.
 * - Implemented Object Pooling to eliminate runtime allocation (GC).
 * - Specialized for 'Particle' class to avoid casting overhead.
 * - Added spatial query methods not present in the original visualization code.
 */

public class SimpleQuadTree {

    private static final List<SimpleQuadTree> pool = new ArrayList<>(1000);
    private static int poolPtr = 0;
    private final List<Particle> objects = new ArrayList<>();
    private int level;
    private float x, y, w, h;
    private int maxObjects;
    private int maxLevels;
    private SimpleQuadTree[] children = null;

    private SimpleQuadTree() {
    }

    public static void resetPool() {
        poolPtr = 0;
    }

    public static SimpleQuadTree obtain(int level, float x, float y, float w, float h, int maxObjects, int maxLevels) {
        SimpleQuadTree qt;
        if (poolPtr < pool.size()) {
            qt = pool.get(poolPtr);
        } else {
            qt = new SimpleQuadTree();
            pool.add(qt);
        }
        poolPtr++;

        qt.init(level, x, y, w, h, maxObjects, maxLevels);
        return qt;
    }

    private void init(int level, float x, float y, float w, float h, int maxObjects, int maxLevels) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.maxObjects = maxObjects;
        this.maxLevels = maxLevels;
        this.objects.clear();
        this.children = null;
    }

    public void insert(Particle p) {
        if (children != null) {
            int index = getIndex(p);
            if (index != -1) {
                children[index].insert(p);
                return;
            }
        }

        objects.add(p);

        if (objects.size() > maxObjects && level < maxLevels) {
            if (children == null) {
                split();
            }
            int i = 0;
            while (i < objects.size()) {
                Particle obj = objects.get(i);
                int index = getIndex(obj);
                if (index != -1) {
                    children[index].insert(obj);
                    objects.remove(i);
                } else {
                    i++;
                }
            }
        }
    }


    public void query(float searchX, float searchY, float searchW, float searchH, List<Particle> result) {

        if (!intersects(searchX, searchY, searchW, searchH)) {
            return;
        }


        for (Particle p : objects) {
            if (p.pos.x >= searchX && p.pos.x < searchX + searchW &&
                    p.pos.y >= searchY && p.pos.y < searchY + searchH) {
                result.add(p);
            }
        }


        if (children != null) {
            for (SimpleQuadTree child : children) {
                child.query(searchX, searchY, searchW, searchH, result);
            }
        }
    }

    private void split() {
        float subW = w / 2;
        float subH = h / 2;

        children = new SimpleQuadTree[4];
        children[0] = obtain(level + 1, x, y, subW, subH, maxObjects, maxLevels); // NW
        children[1] = obtain(level + 1, x + subW, y, subW, subH, maxObjects, maxLevels); // NE
        children[2] = obtain(level + 1, x, y + subH, subW, subH, maxObjects, maxLevels); // SW
        children[3] = obtain(level + 1, x + subW, y + subH, subW, subH, maxObjects, maxLevels); // SE
    }

    private int getIndex(Particle p) {
        float midX = x + (w / 2);
        float midY = y + (h / 2);
        boolean top = (p.pos.y < midY);
        boolean bottom = (p.pos.y >= midY);
        boolean left = (p.pos.x < midX);
        boolean right = (p.pos.x >= midX);

        if (left) {
            if (top) return 0; // NW
            if (bottom) return 2; // SW
        } else if (right) {
            if (top) return 1; // NE
            if (bottom) return 3; // SE
        }
        return -1;
    }

    private boolean intersects(float sx, float sy, float sw, float sh) {
        return !(sx > x + w || sx + sw < x || sy > y + h || sy + sh < y);
    }
}