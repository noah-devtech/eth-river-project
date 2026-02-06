import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.List;

public class Particle {
    private final PVector diff = new PVector(0, 0);
    private final PVector steering = new PVector(0, 0);
    private final PVector sepSum = new PVector(0, 0);
    private final PVector cohSum = new PVector(0, 0);
    private final PVector aliSum = new PVector(0, 0);
    private final PVector desired = new PVector(0, 0);
    PApplet p; // メインのAppletへの参照
    PVector pos;
    PVector vel;
    PVector acc;
    Node targetNode;
    float maxSpeed;
    float maxForce = 0.5f;
    int c;
    float size;
    float slowingRadius = 200; // この距離に入ると減速を開始
    Node srcNode;
    PVector prevPos;

    // コンストラクタの第一引数に PApplet を追加
    public Particle(PApplet p, Node startNode, Node targetNode, float maxSpeed, int startColor, float startSize) {
        this.p = p;
        this.srcNode = startNode;
        this.pos = startNode.pos.copy();
        this.targetNode = targetNode;
        this.maxSpeed = maxSpeed;
        this.c = startColor;
        this.size = startSize;
        this.vel = new PVector(0, 0);
        this.acc = new PVector(0, 0);
        this.prevPos = pos.copy();
    }

    private boolean isNear(PVector target, float r) {
        float dx = target.x - pos.x;
        float dy = target.y - pos.y;
        return (dx * dx + dy * dy) < (r * r);
    }

    void updatePhysics() {
        prevPos.set(pos);
        //オイラー積分
        vel.add(acc);
        vel.limit(maxSpeed);
        pos.add(vel);

        //加速度をリセット
        acc.mult(0);
    }

    private PVector applyDumping() {
        steering.set(0, 0);
        float dampingRadius = 30.0f; // 抵抗が発生し始める距離
        if (isNear(targetNode.pos, dampingRadius)) {
            // 近づくほど「摩擦」を強くする（0.0 = 摩擦なし、1.0 = 強い摩擦）
            // map(現在距離, 0, 150, 最大摩擦, 摩擦なし)
            float dist = PVector.dist(targetNode.pos, pos);
            float damping = PApplet.map(dist, 0, dampingRadius, 0.3f, 0.0f);

            steering.set(vel);
            steering.mult(-1);
            steering.normalize();
            steering.mult(vel.mag() * damping);
        }
        return steering;
    }

    public void calcForces(List<Particle> neighbors) {
        applyForce(applySeeking(targetNode.pos));
        applyForce(applyFlocking(neighbors).mult(0.5f));
        applyForce(applyDumping());


    }

    void draw(PGraphics pg) {
        pg.noFill();
        pg.stroke(c);
        pg.strokeWeight(1.0f);

        pg.line(prevPos.x, prevPos.y, pos.x, pos.y);
    }

    boolean isDead() {
        // p.width, p.height にアクセス
        if (pos.x < 0 || pos.x > p.width || pos.y < 0 || pos.y > p.height) return true;
        return isNear(targetNode.pos, size);
    }

    PVector applySeeking(PVector target) {
        desired.set(target);
        desired.sub(pos);

        float d = desired.mag();
        float minSpeed = maxSpeed * 0.3f;
        // ここで少しノイズを加える（ゆらぎ）
        //float angle = p.noise((float) (pos.x * 0.01), (float) (pos.y * 0.01), (float) (p.frameCount * 0.01)) * TWO_PI;
        //PVector wobble = PVector.fromAngle(angle);
        //wobble.mult(0.5F); // 揺れの強さ

        desired.sub(vel);
        //steer.add(wobble); // 操舵力にゆらぎを足す

        desired.normalize();
        desired.mult(maxSpeed);

        // Arrival Behavior
        if (d < slowingRadius) {
            float m = PApplet.map(d, 0, slowingRadius, minSpeed, maxSpeed);
            desired.mult(m);
        } else {
            desired.mult(maxSpeed);
        }

        desired.limit(maxForce);
        return desired;
    }

    PVector applyFlocking(List<Particle> neighbors) {
        steering.set(0, 0);
        diff.set(0, 0);
        if (neighbors.isEmpty()) return steering;
        sepSum.set(0, 0);
        cohSum.set(0, 0);
        aliSum.set(0, 0);

        float sepRadius = this.size * 0.25f;
        float cohRadius = this.size * 10.0f;
        float aliRadius = this.size * 10.0f;

        int countSep = 0;
        int countCoh = 0;
        int countAli = 0;

        for (Particle other : neighbors) {
            if (other == this) continue;
            if (isNear(other.pos, sepRadius)) {
                diff.set(pos);
                diff.sub(other.pos);
                diff.normalize();
                float d = PVector.dist(pos, other.pos);
                if (0 < d) diff.div(d);
                sepSum.add(diff);
                countSep++;
            }
            if (other.targetNode == this.targetNode) {
                if (isNear(other.pos, cohRadius)) {
                    cohSum.add(other.pos);
                    countCoh++;
                }
                if (isNear(other.pos, aliRadius)) {
                    aliSum.add(other.vel);
                    countAli++;
                }
            }
        }
        if (countSep > 0) {
            sepSum.div(countSep);
            sepSum.setMag(maxSpeed);
            sepSum.limit(maxForce);
            steering.add(sepSum.mult(2.5f));
        }
        if (countCoh > 0) {
            cohSum.div(countCoh);
            steering.add(applySeeking(cohSum).mult(0.8f));
        }
        if (countAli > 0) {
            aliSum.div(countAli);
            aliSum.setMag(maxSpeed);
            aliSum.mult(maxSpeed);
            aliSum.sub(vel);
            aliSum.limit(maxForce);
            steering.add(aliSum.mult(0.5f));
        }
        return steering;
    }

    void applyForce(PVector force) {
        acc.add(force);
    }

    public void makeNodeAlive() {
        if (targetNode != null) {
            targetNode.keepAlive();
        }
        if (srcNode != null) {
            srcNode.keepAlive();
        }
    }


}