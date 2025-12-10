import processing.core.PApplet;
import processing.core.PVector;
import oscP5.*; // oscP5 ライブラリをインポート

import java.util.ArrayList;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends PApplet {

    OscP5 oscP5; // oscP5 のインスタンス

    int listenPort;

    ArrayList<Particle> particles;
    Map<String,Node> nodes;

    // 最後に受信した情報をテキスト表示用（デバッグ用）に保持
    String lastAddress = "N/A";
    String lastProtocol = "N/A";
    int lastLength = 0;
    String lastDetails = "N/A";
    String lastSrcIp = "N/A";
    String lastDstIp = "N/A";
    String lastDirection = "N/A";
    int lastNumber = 0;

    int MAX_RAW_LENGTH = 65535;
    float MIN_P_SIZE = 3;
    float MAX_P_SIZE = 30;

    private final Object lock = new Object();// 排他制御のための専用の鍵

    @Override
    public void settings() {
        size(1000, 800); // ウィンドウサイズ
    }

    @Override
    public void setup() {
        loadConfig();
        background(0);  // 背景を黒に
        //★パーティクルリストを初期化
        particles = new ArrayList<Particle>();
        //OSCリスナーを起動
        oscP5 = new OscP5(this, listenPort);

        nodes = new ConcurrentHashMap<String,Node>();

        println("OSCサーバーをポート " + listenPort + " で起動しました。");
        println("pyshark (main.py) を実行してください...");
    }

    @Override
    public void draw() {
        background(0); // 毎フレーム背景を黒で塗りつぶす

        //--- ★ここからパーティクル処理（レベル1物理演算） ---
        //リストを後ろから逆順にループ（削除しても安全なため）
        synchronized(lock) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update(); // 1. 物理演算（位置を更新）
                p.draw();   // 2. 描画

                // 3. 画面外に出たらリストから削除（メモリ節約）
                if (p.isDead()) {
                    particles.remove(i);
                }
            }
        }
        //--- ★ここまでパーティクル処理 ---
        //--- nodeの表示 ---
        for (Node node : nodes.values()) {
            node.separate(nodes);
            node.seekCenter();
            node.update();
            node.display();
        }
        //--- END:nodeの表示 ---

        // nodes.values().removeIf(n -> n.isDead());

        //--- デバッグ用のテキスト表示（以前のまま） ---
        fill(255, 150); // 少し透明にしてビジュアライゼーションの邪魔にならないように
        textSize(14);
        text("Listening on port: " + listenPort, 20, 30);
        text("Last Address: " + lastAddress, 20, 50);
        text("Protocol: " + lastProtocol, 20, 70);
        text("Length: " + lastLength, 20, 90);
        text("Details: " + lastDetails, 20, 110);
        text("Source IP: " + lastSrcIp, 20, 130);
        text("Dest IP: " + lastDstIp, 20, 150);
        text("Packet NO.: " + lastNumber, 20, 170);
        text("Particle Count: " + particles.size(), 20, 190); // ★現在の粒子数を表示
    }

    /**
     * oscP5がOSCメッセージを受信すると、自動的にこの関数が呼び出されます。
     */
    void oscEvent(OscMessage theOscMessage) {

        //1. アドレス（ラベル）を取得
        lastAddress = theOscMessage.addrPattern();

        //2. 引数を取得
        try {
            lastProtocol = theOscMessage.get(0).stringValue();
            lastLength = theOscMessage.get(1).intValue();
            lastDetails = theOscMessage.get(2).stringValue();
            lastNumber = theOscMessage.get(3).intValue();
            lastSrcIp = theOscMessage.get(4).stringValue();
            lastDstIp = theOscMessage.get(5).stringValue();
            println(theOscMessage.get(4).stringValue() +"->"+theOscMessage.get(5).stringValue());
            lastDirection = packetDirection(lastSrcIp, lastDstIp);

            // --- ★ここからパーティクル生成 ---

            // 粒子の色をプロトコルによって決定
            int particleColor;
            particleColor = switch (lastProtocol) {
                case "tcp" -> color(255, 128, 0); //tcp = オレンジ
                case "dns" -> color(0, 255, 0); // DNS = 緑
                case "tls-hello" -> color(255, 255, 0); // TLS-Hello (SNI) = 黄色
                case "tls" -> color(0, 0, 200); // TLS (暗号化データ) = 暗い青
                case "http" -> color(0, 150, 255); // HTTP = 水色
                case "wg" -> color(64, 0, 128); //WireGuard = 紫
                case "quic" -> color(255, 0, 255); // quic = ピンク
                case "data" -> color(255, 0, 0); // data = 赤
                default -> {
                    println("non-defined protocol:", lastProtocol);
                    yield color(255, 255, 255); // その他 = 白
                }
            };

            Node srcNode = getOrCreateNode(lastSrcIp);
            Node dstNode = getOrCreateNode(lastDstIp);
            // 粒子の大きさをパケット長で決定
            float sqrtLength = sqrt(lastLength);
            float particleSize = map(sqrtLength, sqrt(1), sqrt(MAX_RAW_LENGTH), MIN_P_SIZE, MAX_P_SIZE);
            particleSize = max(MIN_P_SIZE,particleSize);
            float particleSpeed = random(3, 8);
            synchronized (lock) {
                particles.add(new Particle(srcNode, dstNode, particleSpeed, particleColor, particleSize));
            }
            // --- ★ここまでパーティクル生成 ---

        } catch(Exception e) {
            println("OSCメッセージの引数処理中にエラー:", e);
        }
    }
    private void loadConfig(){
        // --- 設定ファイルの読み込み ---
        Properties props = new Properties();
        // "config.properties" をクラスパスのルートから探す
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {

            if (input == null) {
                println("設定ファイル (config.properties) が見つかりません。");
                // デフォルト値を使うか、エラーとして終了するか決める
                listenPort = 12345; // フォールバック
            } else {
                props.load(input); // ファイルを読み込む

                // "app.listenPort" の値を取得 (String) し、整数 (int) に変換
                String portStr = props.getProperty("app.listenPort"); //
                listenPort = Integer.parseInt(portStr);
            }

        } catch (Exception e) {
            println("設定ファイルの読み込み中にエラーが発生しました: " + e.getMessage());
            listenPort = 12345; // エラー時もフォールバック
        }
    }
    private String packetDirection(String srcIp, String dstIp){
        if(isLocal(srcIp) && isLocal(dstIp)){
            return "Local";
        }else if(isLocal(srcIp) && !isLocal(dstIp)){
            return "Outbound";
        }else if(!isLocal(srcIp) && isLocal(dstIp)){
            return "Inbound";
        }else{
            return "Unknown";
        }
    }

    private boolean isLocal(String ip){
        return ip.equals("127.0.0.1") || ip.equals("localhost") || ip.equals("127.0.0.1");
    }
    // === = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // ★★★ パーティクルクラス(ここから下をスケッチの末尾に追加) ★★★
    // ==== = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    class Particle {
        PVector pos; // 位置
        Node targetNode;
        float vel; // 速度
        int c;     // 色
        float size;  // 大きさ

        /**
         * コンストラクタ（設計図）
         * @param startNode スタートのNode (Node)
         * @param targetNode ターゲットのNode (Node)
         * @param particleSpeed 初期速度 (float)
         * @param startColor 粒子の色 (color)
         * @param startSize 粒子の大きさ (float)
         */
        Particle(Node startNode,Node targetNode, float particleSpeed, int startColor, float startSize) {
            this.pos = startNode.pos.copy();
            this.targetNode = targetNode;
            this.vel = particleSpeed;
            this.c = startColor;
            this.size = startSize;
        }

        /**
         * 1. 物理演算（位置の更新）
         */
        void update() {
            PVector dir =PVector.sub(targetNode.pos,pos);
            dir.normalize();
            dir.mult(vel);
            pos.add(dir);
        }

        /**
         * 2. 描画
         */
        void draw() {
            noStroke();
            fill(c, 200); // 粒子を描画（少し透明に）
            ellipse(pos.x, pos.y, size, size);
        }

        /**
         * 3. 画面外に出たかどうかの判定
         * @return 画面の上端または下端より外に出たら true
         */
        boolean isDead() {// ターゲットとの距離が速度（1ステップの移動距離）より小さくなったら到着とみなす
            float d = PVector.dist(pos, targetNode.pos);
            // 画面外判定も念のため残す
            if (pos.x < 0 || pos.x > width || pos.y < 0 || pos.y > height) return true;

            // 到着判定 (スピードより近くまで来たら消す)
            return d < vel;
        }
    }

    class Node {
        String ip;
        PVector pos;
        PVector vel;
        PVector acc;
        float maxspeed = 2.0f;
        float maxforce = 0.05f;
        boolean isLocal;
        float size;
        int nodeColor;

        Node(String ip, PVector startPos, boolean isLocal) {
            this.ip = ip;
            this.pos = startPos.copy();
            this.vel = new PVector(0, 0); // 初期速度は0
            this.acc = new PVector(0, 0); // 初期加速度は0
            this.isLocal = isLocal;
            this.size = isLocal ? 15 : 8;
            this.nodeColor = color(255, 0, 0);
        }
        void applyForce(PVector force) {
            acc.add(force);
        }
        void separate(Map<String, Node> nodes) {
            float desiredseparation = size * 2.0f; // ノードのサイズに合わせて距離を調整
            PVector steer = new PVector(0, 0, 0);
            int count = 0;

            // すべてのノードに対してループ
            for (Node other : nodes.values()) {
                float d = PVector.dist(pos, other.pos);

                // 自分自身ではなく、かつ指定した距離より近い場合
                if ((d > 0) && (d < desiredseparation)) {
                    // 相手から自分へのベクトル（逃げる方向）を計算
                    PVector diff = PVector.sub(pos, other.pos);
                    diff.normalize();
                    diff.div(d);        // 距離が近いほど強く反発するように重み付け
                    steer.add(diff);
                    count++;
                }
            }

            // 平均を計算
            if (count > 0) {
                steer.div((float)count);
            }

            // 操舵力（Steering Force）の計算: Desired - Velocity
            if (steer.mag() > 0) {
                steer.normalize();
                steer.mult(maxspeed);
                steer.sub(vel);
                steer.limit(maxforce); // 力の制限

                // 計算した力を加速度に加える
                applyForce(steer);
            }
        }

        // 画面中央に戻ろうとする力（これがないと分離する力で無限に彼方へ飛んでいきます）
        void seekCenter() {
            float targetX = isLocal ? width * 0.8f : width * 0.2f; // Localは右、Remoteは左へ
            PVector target = new PVector(targetX, height/2);

            PVector desired = PVector.sub(target, pos);
            float d = desired.mag();

            // 中心に近づくほどゆっくりにする (Arrive挙動)
            if (d < 200) {
                float m = map(d, 0, 200, 0, maxspeed);
                desired.setMag(m);
            } else {
                desired.setMag(maxspeed);
            }

            PVector steer = PVector.sub(desired, vel);
            steer.limit(maxforce * 0.1f); // 反発力より少し弱めに設定
            applyForce(steer);
        }

        // 位置の更新
        void update() {
            vel.add(acc);
            vel.limit(maxspeed);
            pos.add(vel);
            acc.mult(0); // 加速度をリセット

            // 画面外に出ないように制限 (Borders)
            pos.x = constrain(pos.x, size, width - size);
            pos.y = constrain(pos.y, size, height - size);
        }

        public void display() {
            noStroke();
            fill(nodeColor);
            ellipse(pos.x, pos.y, size, size);

            fill(255);
            pushStyle();
            textAlign(CENTER);
            text(ip, pos.x, pos.y + size + 10);
            popStyle();
        }

    }
    Node getOrCreateNode(String ip) {
        if (nodes.containsKey(ip)) {
            return nodes.get(ip);
        }

        // 新しいノードの座標計算
        float x, y;
        boolean isLocal = ip.startsWith("127.0.0.1");

        y = random(height * 0.1f, height * 0.9f);

        if (isLocal) {
            // ローカルIP: 画面右側
            x = random(width * 0.8f, width * 0.9f);
        } else {
            // リモートIP: 画面左側
            x = random(width * 0.1f, width * 0.2f);
        }

        PVector pos = new PVector(x, y);
        Node newNode = new Node(ip, pos, isLocal);
        nodes.put(ip, newNode);
        return newNode;
    }
    public static void main(String[] args) {
        // "パッケージ名.クラス名" を文字列で渡す
        PApplet.main("Main");
    }
}