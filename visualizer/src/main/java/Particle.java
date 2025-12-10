import processing.core.PApplet;
import processing.core.PVector;

public class Particle {
    PApplet p; // メインのAppletへの参照
    PVector pos;
    Node targetNode;
    float vel;
    int c;
    float size;

    // コンストラクタの第一引数に PApplet を追加
    public Particle(PApplet p, Node startNode, Node targetNode, float particleSpeed, int startColor, float startSize) {
        this.p = p;
        this.pos = startNode.pos.copy();
        this.targetNode = targetNode;
        this.vel = particleSpeed;
        this.c = startColor;
        this.size = startSize;
    }

    void update() {
        PVector dir = PVector.sub(targetNode.pos, pos);
        dir.normalize();
        dir.mult(vel);
        pos.add(dir);
    }

    void draw() {
        p.noStroke(); // p. をつける
        p.fill(c, 200);
        p.ellipse(pos.x, pos.y, size, size);
    }

    boolean isDead() {
        float d = PVector.dist(pos, targetNode.pos);
        // p.width, p.height にアクセス
        if (pos.x < 0 || pos.x > p.width || pos.y < 0 || pos.y > p.height) return true;
        return d < vel;
    }
}