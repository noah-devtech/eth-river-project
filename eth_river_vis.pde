import oscP5.*; // oscP5 ライブラリをインポート
import netP5.*; // netP5 ライブラリをインポート

OscP5 oscP5; // oscP5 のインスタンス

int listenPort = 12345; // pyshark (main.py) が送信しているポート

// 最後に受信した情報を保持する変数
String lastAddress = "N/A";
String lastProtocol = "N/A";
int lastLength = 0;
String lastDetails = "N/A";
String lastSrcIp = "N/A";
String lastDstIp = "N/A";

void setup() {
    size(800, 600); // ウィンドウサイズ
    background(0);  // 背景を黒に
    
    //OSCリスナーを起動
    oscP5 = new OscP5(this, listenPort);
    
    println("OSCサーバーをポート " + listenPort + " で起動しました。");
    println("pyshark (main.py) を実行してください...");
}

void draw() {
    background(0); // 毎フレーム背景を黒で塗りつぶす
    
    fill(255);     // 文字の色を白に
    textSize(16);
    
    //最後に受信した情報を画面にテキスト表示
    text("Listening on port: " + listenPort, 20, 30);
    text("Last Address: " + lastAddress, 20, 60);
    text("Protocol: " + lastProtocol, 20, 90);
    text("Length: " + lastLength, 20, 120);
    text("Details: " + lastDetails, 20, 150);
    text("source_IP: " + lastSrcIp, 20, 180);
    text("distination_IP: " + lastDstIp, 20, 210);
}

/**
* oscP5がOSCメッセージを受信すると、自動的にこの関数が呼び出されます。
*/
void oscEvent(OscMessage theOscMessage) {
    
    //1. アドレス（ラベル）を取得
    lastAddress = theOscMessage.addrPattern();
    
    //2. 引数を取得 (osc-test.txt の引数の型と順番に合わせる)
    try {
        // 引数 0: プロトコル名 (String)
        lastProtocol = theOscMessage.get(0).stringValue();
        
        // 引数 1: パケット長 (Int)
        lastLength = theOscMessage.get(1).intValue();
        
        // 引数 2: 詳細 (String)
        lastDetails = theOscMessage.get(2).stringValue();
        
        lastSrcIp = theOscMessage.get(4).stringValue();
        
        lastDstIp = theOscMessage.get(5).stringValue();
        
    } catch(Exception e) {
        println("OSCメッセージの引数処理中にエラー:", e);
    }
    
    //3. 受信ログをコンソールに出力 (osc_receiver.py と同様)
    println("[OSC受信] アドレス: " + lastAddress);
    println("  データ:");
    println("    引数 0 (Protocol): " + lastProtocol);
    println("    引数 1 (Length): " + lastLength);
    println("    引数 2 (Details): " + lastDetails);
    println("    引数 4 (Details): " + lastSrcIp);
    println("    引数 5 (Details): " + lastDstIp);
}
