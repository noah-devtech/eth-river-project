import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

public class Particle {
    PApplet p; // メインのAppletへの参照
    PVector pos;
    PVector vel;
    PVector acc;
    Node targetNode;
    float maxSpeed;
    float maxForce = 0.5f;
    int c;
    float size;

    // コンストラクタの第一引数に PApplet を追加
    public Particle(PApplet p, Node startNode, Node targetNode, float maxSpeed, int startColor, float startSize) {
        this.p = p;
        this.pos = startNode.pos.copy();
        this.targetNode = targetNode;
        this.maxSpeed = maxSpeed;
        this.c = startColor;
        this.size = startSize;
        this.vel = new PVector(0, 0);
        this.acc = new PVector(0, 0);
    }

    void update(ArrayList<Particle> particles) {
        PVector steer = seek(targetNode.pos);
        PVector separation = separate(particles);
        PVector cohesion = cohesion(particles);
        PVector alignment = alignment(particles);
        applyForce(steer);
        applyForce(separation.mult(2.5f));
        applyForce(alignment.mult(0.8f));
        applyForce(cohesion.mult(0.5f));

        //オイラー積分
        vel.add(acc);
        vel.limit(maxSpeed);
        pos.add(vel);

        //加速度をリセット
        acc.mult(0);
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
        return d < maxSpeed;
    }

    PVector seek(PVector target) {
        PVector desired = PVector.sub(target, pos);
        desired.normalize();
        desired.mult(maxSpeed);
        PVector sum = PVector.sub(desired, vel);
        sum.limit(maxForce);
        return sum;
    }

    PVector separate(ArrayList<Particle> particles) {
        float desiredSeparation = this.size;
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
}