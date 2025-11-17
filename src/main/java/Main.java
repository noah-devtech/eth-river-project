import processing.core.PApplet;
import processing.core.PVector;
import oscP5.*; // oscP5 ライブラリをインポート
import netP5.*; // netP5 ライブラリをインポート
import java.util.ArrayList;
import java.io.InputStream;
import java.util.Properties;

public class Main extends PApplet {

    OscP5 oscP5; // oscP5 のインスタンス

    int listenPort;

    // ★パーティクル（粒子）のリストを作成
    ArrayList<Particle> particles;

    // 最後に受信した情報をテキスト表示用（デバッグ用）に保持
    String lastAddress = "N/A";
    String lastProtocol = "N/A";
    int lastLength = 0;
    String lastDetails = "N/A";
    String lastSrcIp = "N/A";
    String lastDstIp = "N/A";

    @Override
    public void settings() {
        size(800, 600); // ウィンドウサイズ
    }

    @Override
    public void setup() {
        loadConfig();
        background(0);  // 背景を黒に
        //★パーティクルリストを初期化
        particles = new ArrayList<Particle>();
        //OSCリスナーを起動
        oscP5 = new OscP5(this, listenPort);

        println("OSCサーバーをポート " + listenPort + " で起動しました。");
        println("pyshark (main.py) を実行してください...");
    }

    @Override
    public void draw() {
        background(0); // 毎フレーム背景を黒で塗りつぶす

        //--- ★ここからパーティクル処理（レベル1物理演算） ---
        //リストを後ろから逆順にループ（削除しても安全なため）
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update(); // 1. 物理演算（位置を更新）
            p.draw();   // 2. 描画

            // 3. 画面外に出たらリストから削除（メモリ節約）
            if (p.isDead()) {
                particles.remove(i);
            }
        }
        //--- ★ここまでパーティクル処理 ---


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
        text("Particle Count: " + particles.size(), 20, 170); // ★現在の粒子数を表示
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
            lastSrcIp = theOscMessage.get(4).stringValue();
            lastDstIp = theOscMessage.get(5).stringValue();
            println(theOscMessage.get(4).stringValue() +"->"+theOscMessage.get(5).stringValue());
            println(packetDirection(lastSrcIp, lastDstIp));

            // --- ★ここからパーティクル生成 ---
            // 3. 受信したデータで新しいParticleを生成

            // 粒子の色をプロトコルによって決定
            int particleColor = color(100); // デフォルト (灰色)
            // QUIC-Hello = マゼンタ
            particleColor = switch (lastProtocol) {
                case "DNS" -> color(0, 255, 0); // DNS = 緑

                case "TLS-Hello" -> color(255, 255, 0); // TLS-Hello (SNI) = 黄色

                case "TLS" -> color(0, 0, 200); // TLS (暗号化データ) = 暗い青

                case "HTTP Request" -> color(0, 150, 255); // HTTPリクエスト = 水色

                case "wg" -> color(64, 0, 128); //WireGuard = 紫

                case "QUIC-Hello" -> color(255, 0, 255); // = ピンク
                default -> color(255, 255, 255); // その他 = 白
            };

            // 粒子の大きさをパケット長で決定 (例)
            float particleSize = map(lastLength, 40, 1500, 2, 20); // 40-1500バイトを2-20ピクセルに変換

            // 川の源流（画面上部のランダムなX座標）に粒子を追加
            PVector startPos = new PVector(random(width), 0);
            particles.add(new Particle(startPos, particleColor, particleSize));
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
        return ip.equals("127.0.0.1") || ip.equals("localhost") || ip.equals("160.194.177.107");
    }
    // === = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    // ★★★ パーティクルクラス(ここから下をスケッチの末尾に追加) ★★★
    // ==== = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
    class Particle {

        PVector pos; // 位置
        PVector vel; // 速度
        int c;     // 色
        float size;  // 大きさ

        // コンストラクタ（設計図）
        Particle(PVector startPos, int startColor, float startSize) {
            pos = startPos.copy(); // 初期位置
            c = startColor;
            size = startSize;

            // 物理演算（川の流れ）
            // 真下（Y+方向）に、少しランダムな速度を与える
            vel = new PVector(0,random(1.0F, 3.0F));
        }

        //1.物理演算（位置の更新）
        void update() {
            pos.add(vel); // 位置 ＝ 現在の位置 ＋ 速度
        }

        //2.描画
        void draw() {
            noStroke();
            fill(c, 200); // 粒子を描画（少し透明に）
            ellipse(pos.x, pos.y, size, size);
        }

        //3.画面外に出たかどうかの判定
        boolean isDead() {
            // 画面の下端より下に行ったら
            return pos.y > height;
        }
    }


    public static void main(String[] args) {
        // "パッケージ名.クラス名" を文字列で渡す
        PApplet.main("Main");
    }
}