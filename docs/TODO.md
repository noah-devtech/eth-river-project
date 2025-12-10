# プロジェクト全体 TODO リスト

このファイルは Ethernet リバー・プロジェクトの全体的なタスク・進捗管理用です。

---

## 現在の TODO リスト

-   [ ] P2-1: TCP ハンドシェイク処理の追加（tcp_handler.py、SYN/FIN/RST フラグの判定ロジック）
-   [ ] P3-1: QUIC プロトコル対応（udp_handler.py、QUIC SNI 取得）
-   [ ] P3-Vis: TCP 制御パケットの可視化（Processing 側で色・寿命を変える描画ロジック）
-   [ ] P2-II/P2-III: Kinect 連携の実装（Kinect 座標 OSC 送信、Processing 側での介入ロジック）
-   [ ] P4-NodeVis: ノードベース可視化システムの設計・実装
-   [ ] 未定義プロトコル（DATA, STUN 等）の暫定処理（default_handler で無視 or 記録）
-   [ ] パフォーマンス改善（pyshark のボトルネック調査・最適化）
-   [ ] コードリファクタリング（dns_handler.py, tcp_handler.py 等の TODO 解消）

---

## 参考: 旧 TODO リストの出典

-   `.github/copilot-instructions.md`
-   `docs/instructions_for_analyzer.md`
-   `docs/instructions_for_visualizer.md`
-   `docs/DECISIONS.md`

---

## 更新方法

-   新しいタスクや完了済みタスクはこのファイルに追記・修正してください。
-   各担当ディレクトリの細かい TODO は、必要に応じて個別の`TODO.md`に分けても構いません。
