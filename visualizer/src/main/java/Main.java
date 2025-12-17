import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends PApplet {

    private static final String[] TARGET_PREFIXES = {
            "10.0.0.0/8",
            "172.0.0.0/12",
            "192.168.0.0/16",
            "160.194.177.107/32"
    };
    private final Object lock = new Object();
    OscP5 oscP5;
    int listenPort;
    ArrayList<Particle> particles;
    Map<String, Node> nodes;
    String lastAddress = "N/A";
    String lastProtocol = "N/A";
    int lastLength = 0;
    String lastDetails = "N/A";
    String lastSrcIp = "N/A";
    String lastDstIp = "N/A";
    String lastDirection = "N/A";
    int lastNumber = 0;
    int MAX_RAW_LENGTH = 2000;
    float MIN_P_SIZE = 1;
    float MAX_P_SIZE = 30;

    PGraphics fadeLayer;

    public static void main(String[] args) {
        PApplet.main("Main");
    }

    @Override
    public void settings() {
        size(1000, 800);
    }

    @Override
    public void setup() {
        loadConfig();
        background(0);
        particles = new ArrayList<Particle>();
        oscP5 = new OscP5(this, listenPort);
        nodes = new ConcurrentHashMap<String, Node>();
        fadeLayer = createGraphics(width, height);

        println("OSCサーバーをポート " + listenPort + " で起動しました。");
        println("pyshark (main.py) を実行してください...");
    }

    @Override
    public void draw() {
        fadeLayer.beginDraw();
        fadeLayer.blendMode(SUBTRACT);

        fadeLayer.noStroke();
        fadeLayer.fill(10);
        fadeLayer.rect(0, 0, width, height);

        fadeLayer.blendMode(ADD);


        synchronized (lock) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(particles);
                p.draw(fadeLayer);
                p.makeNodeAlive();
                if (p.isDead()) {
                    particles.remove(i);
                }
            }
        }
        fadeLayer.endDraw();
        background(0);
        blendMode(BLEND);
        image(fadeLayer, 0, 0);

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

        fill(255, 150);
        textSize(14);
        text("Listening on port: " + listenPort, 20, 30);
        text("Last Address: " + lastAddress, 20, 50);
        text("Protocol: " + lastProtocol, 20, 70);
        text("Length: " + lastLength, 20, 90);
        text("Details: " + lastDetails, 20, 110);
        text("Source IP: " + lastSrcIp, 20, 130);
        text("Dest IP: " + lastDstIp, 20, 150);
        text("Packet NO.: " + lastNumber, 20, 170);
        text("Direction: " + lastDirection, 20, 190);
        text("Particle Count: " + particles.size(), 20, 210);
    }

    void oscEvent(OscMessage theOscMessage) {
        lastAddress = theOscMessage.addrPattern();
        try {
            lastProtocol = theOscMessage.get(0).stringValue();
            lastLength = theOscMessage.get(1).intValue();
            lastDetails = theOscMessage.get(2).stringValue();
            lastNumber = theOscMessage.get(3).intValue();
            lastSrcIp = theOscMessage.get(4).stringValue();
            lastDstIp = theOscMessage.get(5).stringValue();
            println(theOscMessage.get(4).stringValue() + "->" + theOscMessage.get(5).stringValue());
            lastDirection = packetDirection(lastSrcIp, lastDstIp);

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
            float particleSize = map(sqrtLength, sqrt(1), sqrt(MAX_RAW_LENGTH), MIN_P_SIZE, MAX_P_SIZE);
            particleSize = max(MIN_P_SIZE, particleSize);
            float particleSpeed = 5;

            synchronized (lock) {
                // 変更点: 第一引数に this を渡す
                particles.add(new Particle(this, srcNode, dstNode, particleSpeed, particleColor, particleSize));
            }

        } catch (Exception e) {
            println("OSCメッセージの引数処理中にエラー:", e);
        }
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                println("設定ファイル (config.properties) が見つかりません。");
                listenPort = 12345;
            } else {
                props.load(input);
                String portStr = props.getProperty("app.listenPort");
                listenPort = Integer.parseInt(portStr);
            }
        } catch (Exception e) {
            println("設定ファイルの読み込み中にエラーが発生しました: " + e.getMessage());
            listenPort = 12345;
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

        PVector pos = new PVector(x, y);
        // 変更点: 第一引数に this を渡す
        Node newNode = new Node(this, ip, pos, isLocal);
        nodes.put(ip, newNode);
        return newNode;
    }
}