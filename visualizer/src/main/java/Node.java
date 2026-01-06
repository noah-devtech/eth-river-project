import processing.core.PApplet;
import processing.core.PVector;

import java.util.Map;

public class Node {
    PApplet p; // メインのAppletへの参照
    String ip;
    PVector pos;
    PVector vel;
    PVector acc;
    float maxspeed = 2.0f;
    float maxforce = 0.05f;
    boolean isLocal;
    float size;
    int nodeColor;
    int life = 300;
    int maxLife = 300;

    // コンストラクタの第一引数に PApplet を追加
    public Node(PApplet p, String ip, PVector startPos, boolean isLocal) {
        this.p = p;
        this.ip = ip;
        this.pos = startPos.copy();
        this.vel = new PVector(0, 0);
        this.acc = new PVector(0, 0);
        this.isLocal = isLocal;
        this.size = isLocal ? 15 : 8;
        this.nodeColor = p.color(255, 0, 0);
    }

    void applyForce(PVector force) {
        acc.add(force);
    }

    void separate(Map<String, Node> nodes) {
        float desiredseparation = size * 2.0f;
        PVector steer = new PVector(0, 0, 0);
        int count = 0;

        for (Node other : nodes.values()) {
            float d = PVector.dist(pos, other.pos);
            if ((d > 0) && (d < desiredseparation)) {
                PVector diff = PVector.sub(pos, other.pos);
                diff.normalize();
                diff.div(d);
                steer.add(diff);
                count++;
            }
        }

        if (count > 0) {
            steer.div((float) count);
        }

        if (steer.mag() > 0) {
            steer.normalize();
            steer.mult(maxspeed);
            steer.sub(vel);
            steer.limit(maxforce);
            applyForce(steer);
        }
    }

    void seekCenter() {
        // width, height は p.width, p.height に
        float targetX = isLocal ? p.width * 0.8f : p.width * 0.2f;
        PVector target = new PVector(targetX, p.height / 2);

        PVector desired = PVector.sub(target, pos);
        float d = desired.mag();

        if (d < 200) {
            float m = PApplet.map(d, 0, 200, 0, maxspeed); // staticなmapを使うか、p.mapを使う
            desired.setMag(m);
        } else {
            desired.setMag(maxspeed);
        }

        PVector steer = PVector.sub(desired, vel);
        steer.limit(maxforce * 0.1f);
        applyForce(steer);
    }

    void update() {
        vel.add(acc);
        vel.limit(maxspeed);
        //pos.add(vel);
        acc.mult(0);
        pos.y += vel.y;
        // p.width, p.height, p.constrain
        pos.x = PApplet.constrain(pos.x, size, p.width - size);
        pos.y = PApplet.constrain(pos.y, size, p.height - size);
    }

    public void display() {
        float nodeAlpha = PApplet.map(life, 300, 0, 255, 0);
        p.noStroke();
        p.fill(nodeColor, nodeAlpha);
        p.ellipse(pos.x, pos.y, size, size);

        p.fill(255, nodeAlpha);
        p.pushStyle();
        p.textAlign(PApplet.CENTER); // 定数はクラス名アクセス可
        p.text(ip, pos.x, pos.y + size + 10);
        p.popStyle();
    }

    public boolean isDead() {
        return life <= 0;
    }

    void keepAlive() {
        this.life = maxLife;
    }
}