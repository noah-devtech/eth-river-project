import oscP5.*; 
import netP5.*; 
import java.util.ArrayList; // ★リストを管理するために追加

OscP5 oscP5; 
int listenPort = 12345; 

// ★パーティクル（粒子）のリストを作成
ArrayList<Particle> particles;

// 最後に受信した情報をテキスト表示用（デバッグ用）に保持
String lastAddress = "N/A";
String lastProtocol = "N/A";
int lastLength = 0;
String lastDetails = "N/A";
String lastSrcIp = "N/A";
String lastDstIp = "N/A";


void setup() {
    size(800, 600); 
    background(0);  
    
    //★パーティクルリストを初期化
    particles = new ArrayList<Particle>();
    
    //OSCリスナーを起動
    oscP5 = new OscP5(this, listenPort);
    println("OSCサーバーをポート " + listenPort + " で起動しました。");
}

void draw() {
    background(0); 
    
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
* OSCメッセージ受信時の処理
*/
void oscEvent(OscMessage theOscMessage) {
    
    //1. アドレス（ラベル）を取得
    lastAddress = theOscMessage.addrPattern();
    
    //2. 引数を取得
    try {
        lastProtocol = theOscMessage.get(0).stringValue();
        lastLength = theOscMessage.get(1).intValue();
        lastDetails = theOscMessage.get(2).stringValue();
        lastSrcIp = theOscMessage.get(3).stringValue();
        lastDstIp = theOscMessage.get(4).stringValue();
        
        // --- ★ここからパーティクル生成 ---
        // 3. 受信したデータで新しいParticleを生成
        
        // 粒子の色をプロトコルによって決定
        color particleColor = color(100); // デフォルト (灰色)
        if (lastProtocol.equals("DNS")) {
            particleColor = color(0, 255, 0); // DNS = 緑
        } else if (lastProtocol.equals("TLS-Hello")) {
            particleColor = color(255, 255, 0); // TLS-Hello (SNI) = 黄色
        } else if (lastProtocol.equals("TLS")) {
            particleColor = color(0, 0, 200); // TLS (暗号化データ) = 暗い青
        } else if (lastProtocol.equals("HTTP Request")) {
            particleColor = color(0, 150, 255); // HTTPリクエスト = 水色
        } else if (lastProtocol.equals("TCP-SYN")) {
            particleColor = color(255, 255, 255); // TCP-SYN = 白
        } else if (lastProtocol.equals("QUIC-Hello")) {
            particleColor = color(255, 0, 255); // QUIC-Hello = マゼンタ
        }
        
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


// === = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
// ★★★ パーティクルクラス(ここから下をスケッチの末尾に追加) ★★★
// ==== = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
class Particle {
    
    PVector pos; // 位置
    PVector vel; // 速度
    color c;     // 色
    float size;  // 大きさ
    
    // コンストラクタ（設計図）
    Particle(PVector startPos, color startColor, float startSize) {
        pos = startPos.copy(); // 初期位置
        c = startColor;
        size = startSize;
        
        // 物理演算（川の流れ）
        // 真下（Y+方向）に、少しランダムな速度を与える
        vel = new PVector(0,random(1.0, 3.0)); 
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
        if (pos.y > height) { // 画面の下端より下に行ったら
            return true;
        } else {
            return false;
        }
    }
}