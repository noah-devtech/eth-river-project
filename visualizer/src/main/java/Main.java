import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import io.github.cdimascio.dotenv.Dotenv;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main extends PApplet {

    private final Object lock = new Object();
    private final ThreadLocal<List<Particle>> threadLocalBuffer = ThreadLocal.withInitial(() -> new ArrayList<>(500));
    OscP5 oscP5;
    int listenPort;
    ArrayList<Particle> particles;
    ConcurrentLinkedQueue<Particle> newParticleQueue = new ConcurrentLinkedQueue<>();
    Map<String, Node> nodes;
    String lastAddress = "N/A";
    String lastProtocol = "N/A";
    int lastLength = 0;
    String lastSrcIp = "N/A";
    String lastDstIp = "N/A";
    int lastNumber = 0;
    int MAX_RAW_LENGTH = 1500;
    float MIN_P_SIZE = 1;
    float MAX_P_SIZE = 30;
    int counter = 0;
    SimpleQuadTree quadTree;
    PGraphics particleLayer;
    PGraphics nodeLayer;
    boolean isDebug;
    DebugWindow debugWindow = new DebugWindow(this);
    private String[] TARGET_PREFIXES;

    public static void main(String[] args) {
        PApplet.main("Main");
    }

    @Override
    public void settings() {
        size(1200, 1000, P2D);
    }

    @Override
    public void setup() {
        PApplet.runSketch(new String[]{"DebugWindow"}, debugWindow);
        Particle.preAllocate(this, 20000);
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();


        listenPort = Integer.parseInt(dotenv.get("LISTENING_PORT", "12345"));
        String prefixes = dotenv.get("TARGET_PREFIXES", "10.0.0.0/8,172.16.0.0/12,192.168.0.0/16");
        TARGET_PREFIXES = prefixes.split(",");
        isDebug = Boolean.parseBoolean(dotenv.get("DEBUG_MODE", "false"));
        if (isDebug) {
            debugWindow.show();
        } else {
            debugWindow.hide();
        }
        background(0);
        particles = new ArrayList<>(20000);
        newParticleQueue = new ConcurrentLinkedQueue<>();
        oscP5 = new OscP5(this, listenPort);
        nodes = new ConcurrentHashMap<String, Node>();
        nodeLayer = createGraphics(width * pixelDensity, height * pixelDensity, P2D);
        particleLayer = createGraphics(width * pixelDensity, height * pixelDensity, P2D);
        pixelDensity(displayDensity());
        windowResizable(true);
        println("OSCサーバーをポート " + listenPort + " で起動しました。");
        println("pyshark (main.py) を実行してください...");
        PFont font = createFont("BIZ UDPゴシック", 14);
        textFont(font);
    }

    @Override
    public void draw() {
        while (!newParticleQueue.isEmpty()) {
            particles.add(newParticleQueue.poll());
        }
        if (keyPressed && key == 'f') {
            for (int i = 0; i < 100; i++) {
                spawnDebugParticle();
            }
        }
        particleLayer.beginDraw();
        particleLayer.scale(pixelDensity);
        particleLayer.blendMode(SUBTRACT);

        particleLayer.noStroke();
        particleLayer.fill(3, 255);
        particleLayer.rect(0, 0, width, height);
        particleLayer.blendMode(BLEND);

        particleLayer.noStroke();
        particleLayer.fill(0, 10);
        particleLayer.rect(0, 0, width, height);

        particleLayer.blendMode(ADD);
        SimpleQuadTree.resetPool();
        quadTree = SimpleQuadTree.obtain(0, 0, 0, width, height, 50, 5);
        for (Particle p : particles) {
            quadTree.insert(p);
        }
        float r = 50.0f;

        particles.parallelStream().forEach(p -> {
            List<Particle> queryBuffer = threadLocalBuffer.get();
            queryBuffer.clear();
            quadTree.query(p.pos.x - r, p.pos.y - r, r * 2, r * 2, queryBuffer);
            p.calcForces(queryBuffer);
        });


        particleLayer.beginShape(LINES);
        particleLayer.noFill();
        particleLayer.strokeWeight(1.0f);
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.updatePhysics();


            particleLayer.stroke(p.c);

            particleLayer.line(p.prevPos.x, p.prevPos.y, p.pos.x, p.pos.y);


            p.makeNodeAlive();
            if (p.isDead()) {
                int lastIndex = particles.size() - 1;
                if (i != lastIndex) {
                    Particle lastParticle = particles.remove(lastIndex);
                    particles.set(i, lastParticle);
                } else {
                    particles.remove(lastIndex);
                }
                Particle.recycle(p);
                i--;
            }
        }
        particleLayer.endShape();
        particleLayer.endDraw();
        background(0);
        blendMode(BLEND);
        image(particleLayer, 0, 0, width, height);

        nodeLayer.beginDraw();
        nodeLayer.scale(pixelDensity);
        nodeLayer.clear();

        nodeLayer.blendMode(ADD);

        for (Node node : nodes.values()) {
            node.separate(nodes);
            node.seekCenter();
            node.update();
            node.display();
            node.life--;
            if (node.isDead()) {
                nodes.remove(node.ip);
            }
        }

        nodeLayer.endDraw();
        image(nodeLayer, 0, 0, width, height);
    }

    void oscEvent(OscMessage theOscMessage) {
        lastAddress = theOscMessage.addrPattern();
        try {
            lastProtocol = theOscMessage.get(0).stringValue();
            lastLength = theOscMessage.get(1).intValue();
            lastNumber = theOscMessage.get(2).intValue();
            lastSrcIp = theOscMessage.get(3).stringValue();
            lastDstIp = theOscMessage.get(4).stringValue();
            println(theOscMessage.get(3).stringValue() + "->" + theOscMessage.get(4).stringValue());

            int particleColor;
            particleColor = switch (lastProtocol) {
                case "tcp" -> color(255, 128, 0);
                case "dns" -> color(0, 255, 0);
                case "tls-hello" -> color(255, 255, 0);
                case "tls" -> color(0, 0, 200);
                case "http" -> color(0, 150, 255);
                case "wg" -> color(64, 0, 128);
                case "quic" -> color(255, 0, 255);
                case "data" -> color(255, 0, 0);
                default -> {
                    println("non-defined protocol:", lastProtocol);
                    yield color(255, 255, 255);
                }
            };

            Node srcNode = getOrCreateNode(lastSrcIp);
            Node dstNode = getOrCreateNode(lastDstIp);
            float sqrtLength = sqrt(lastLength);
            float particleSize = map(sqrtLength, 1, sqrt(MAX_RAW_LENGTH), MIN_P_SIZE, MAX_P_SIZE);
            particleSize = max(MIN_P_SIZE, particleSize);
            float particleSpeed = 5;


            newParticleQueue.add(Particle.obtain(srcNode, dstNode, particleSpeed, particleColor, particleSize));
            synchronized (lock) {
                counter++;
            }

        } catch (Exception e) {
            println("OSCメッセージの引数処理中にエラー:", e);
        }
    }

    private String packetDirection(String srcIp, String dstIp) {
        if (isLocal(srcIp) && isLocal(dstIp)) return "Local";
        else if (isLocal(srcIp) && !isLocal(dstIp)) return "Outbound";
        else if (!isLocal(srcIp) && isLocal(dstIp)) return "Inbound";
        else return "Unknown";
    }

    private boolean isLocal(String ip) {
        try {
            IPAddress address = new IPAddressString(ip).toAddress();
            // 指定された範囲に属するかチェック
            for (String prefix : TARGET_PREFIXES) {
                if (new IPAddressString(prefix).toAddress().contains(address)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    Node getOrCreateNode(String ip) {
        if (nodes.containsKey(ip)) {
            return nodes.get(ip);
        }

        float x, y;
        boolean isLocal = isLocal(ip);
        y = random(height * 0.1f, height * 0.9f);

        if (isLocal) {
            x = random(width * 0.8f, width * 0.9f);
        } else {
            x = random(width * 0.1f, width * 0.2f);
        }
        Node newNode = new Node(this, ip, x, y, isLocal);
        nodes.put(ip, newNode);
        return newNode;
    }

    @Override
    public void windowResized() {
        particleLayer = createGraphics(width * pixelDensity, height * pixelDensity, P2D);
        nodeLayer = createGraphics(width * pixelDensity, height * pixelDensity, P2D);
    }

    @Override
    public void keyPressed() {
        if (key == 'e' || key == 'E') {
            exit();
        }
        if (key == 'c' || key == 'C') {
            counter = 0;
        }
        if (key == 'd' || key == 'D') {
            onToggleDebug();
        }
    }

    void spawnDebugParticle() {
        if (nodes.isEmpty()) return;
        List<Node> nodeList = new ArrayList<>(nodes.values());
        Node srcNode = nodeList.get((int) random(nodeList.size()));
        Node dstNode = nodeList.get((int) random(nodeList.size()));
        newParticleQueue.add(Particle.obtain(srcNode, dstNode, 5.0f, color(0, 255, 255), random(MIN_P_SIZE, MAX_P_SIZE)));
    }

    public void onToggleDebug() {
        isDebug = !isDebug;
        if (isDebug) {
            debugWindow.show();
        } else {
            debugWindow.hide();
        }
    }
}