import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.util.ArrayList;

import static processing.core.PConstants.TWO_PI;

public class Particle {
    PApplet p; // メインのAppletへの参照
    PVector pos;
    PVector prevPos;
    PVector vel;
    PVector acc;
    Node targetNode;
    float maxSpeed;
    float maxForce = 0.5f;
    int c;
    float size;
    float slowingRadius = 200; // この距離に入ると減速を開始
    Node srcNode;
    ArrayList<PVector> history = new ArrayList<PVector>();
    int maxHistory = 5;

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
    }

    void update(ArrayList<Particle> particles) {
        history.add(pos.copy());
        if (history.size() > maxHistory) {
            history.remove(0);
        }
        PVector steer = seek(targetNode.pos);
        PVector separation = separate(particles);
        PVector cohesion = cohesion(particles);
        PVector alignment = alignment(particles);
        applyForce(steer);
        applyForce(separation.mult(2.5f));
        applyForce(alignment.mult(0.8f));
        applyForce(cohesion.mult(0.5f));


        float dist = PVector.dist(targetNode.pos, pos);
        float dampingRadius = 200.0f; // 抵抗が発生し始める距離

        if (dist < dampingRadius) {
            // 近づくほど「摩擦」を強くする（1.0 = 摩擦なし、0.9 = 強い摩擦）
            // map(現在距離, 0, 150, 最大摩擦, 摩擦なし)
            float damping = PApplet.map(dist, 0, dampingRadius, 0.85f, 1.0f);

            vel.mult(damping);
        }
        //オイラー積分
        vel.add(acc);
        vel.limit(maxSpeed);
        pos.add(vel);

        //加速度をリセット
        acc.mult(0);
    }

    void draw(PGraphics pg) {
        pg.noFill();
        pg.stroke(c);
        pg.strokeWeight(1.0f);

        pg.beginShape();
        for (PVector pos : history) {
            pg.vertex(pos.x, pos.y);
        }
        pg.vertex(pos.x, pos.y);
        pg.endShape();
    }

    boolean isDead() {
        float d = PVector.dist(pos, targetNode.pos);
        // p.width, p.height にアクセス
        if (pos.x < 0 || pos.x > p.width || pos.y < 0 || pos.y > p.height) return true;
        return d < (this.size * 0.75f);
    }

    PVector seek(PVector target) {
        PVector desired = PVector.sub(target, pos);

        float d = desired.mag();
        float minSpeed = maxSpeed * 0.3f;
        // ここで少しノイズを加える（ゆらぎ）
        float angle = p.noise((float) (pos.x * 0.01), (float) (pos.y * 0.01), (float) (p.frameCount * 0.01)) * TWO_PI;
        PVector wobble = PVector.fromAngle(angle);
        wobble.mult(0.5F); // 揺れの強さ

        PVector steer = PVector.sub(desired, vel);
        steer.add(wobble); // 操舵力にゆらぎを足す

        desired.normalize();
        desired.mult(maxSpeed);

        // Arrival Behavior
        if (d < slowingRadius) {
            float m = PApplet.map(d, 0, slowingRadius, minSpeed, maxSpeed);
            desired.mult(m);
        } else {
            desired.mult(maxSpeed);
        }

        //PVector steer = PVector.sub(desired, vel);
        steer.limit(maxForce);
        return steer;
    }

    PVector separate(ArrayList<Particle> particles) {
        float desiredSeparation = this.size * 0.1f;
        PVector sum = new PVector(0, 0);
        int count = 0;
        for (Particle other : particles) {
            float d = PVector.dist(pos, other.pos);
            if ((d > 0) && (d < desiredSeparation)) {
                PVector diff = PVector.sub(pos, other.pos);
                diff.div(d);
                sum.add(diff);
                count++;
            }
        }
        if (count > 0) {
            sum.div(count);
            sum.normalize();
            sum.mult(this.maxSpeed);
            sum.limit(this.maxForce);
        }
        return sum;
    }

    PVector cohesion(ArrayList<Particle> particles) {
        float neighborhoodRadius = this.size * 10.0f;
        PVector centerOfMass = new PVector(0, 0);
        int count = 0;

        for (Particle other : particles) {
            float d = PVector.dist(pos, other.pos);

            if ((d > 0) && (d < neighborhoodRadius) && (other.targetNode == this.targetNode)) {
                centerOfMass.add(other.pos);
                count++;
            }
        }

        if (count > 0) {
            centerOfMass.div(count);
            return seek(centerOfMass);
        }

        return new PVector(0, 0);
    }

    PVector alignment(ArrayList<Particle> particles) {
        float neighborhoodRadius = this.size * 5.0f;
        PVector sumVelocity = new PVector(0, 0);
        int count = 0;

        for (Particle other : particles) {
            float d = PVector.dist(pos, other.pos);

            if ((d > 0) && (d < neighborhoodRadius) && (other.targetNode == this.targetNode)) {
                sumVelocity.add(other.vel);
                count++;
            }
        }

        if (count > 0) {
            sumVelocity.div(count);
            sumVelocity.normalize();
            sumVelocity.mult(maxSpeed);

            PVector steer = PVector.sub(sumVelocity, vel);
            steer.limit(maxForce); // 操舵力を制限
            return steer;
        }

        return new PVector(0, 0);

    }

    void applyForce(PVector force) {
        PVector f = force.copy();
        acc.add(f);
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