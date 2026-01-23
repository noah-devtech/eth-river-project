# Analyzeが実際に触れるデータ

Analyzeが実際に触れるデータは、以下の通りです。

## パケット全体のメタデータ(`main.py`)

パケットキャプチャ時に、パケットそのものの属性情報を取得しています。

* パケット番号: (`packet.number`)
* タイムスタンプ: (`packet.frame_info.time_epoch`)
* パケット全体の長さ: (`packet.length`)

## レイヤー2：データリンク層（`pipeline/link_layer.py`）

イーサネットフレームのヘッダー情報のみを取得しています。

* 送信元MACアドレス: (`eth.src`)

* 宛先MACアドレス: (`eth.dst`)

## レイヤー3：ネットワーク層（`pipeline/network_layer.py`）

IPヘッダーの情報のみを取得しています。

* 送信元IPアドレス: (`ip.src`)

* 宛先IPアドレス: (`ip.dst`)

* プロトコル番号: (`ip.proto`)（TCPかUDPかを判断するため）

## レイヤー4：トランスポート層（`pipeline/transport_layer/*.py`）

TCP/UDPヘッダーの情報と、データの有無を判断するためのサイズ情報のみを取得しています。

* ポート番号: 送信元(`srcport`) / 宛先(`dstport`)

* TCPフラグ: SYN, FIN, ACK（接続状態の判定用）

* ペイロード長:
  * TCP: (`tcp.len`)（データが含まれているか長さだけで判定）
  * UDP: (`udp.length`)（ヘッダーサイズと比較してデータ有無を判定）

## レイヤー7：アプリケーション層（`pipeline/application_layer/*.py`）

HTTP, DNS, TLSなどのプロトコルについては、**「そのレイヤーが存在するかどうか（layer_name）」** という情報だけに触れています。URL、ホスト名、クエリ内容などの具体的な中身は取得していません。

## 最終出力・外部送信データ（`utils.py`）

最終的にコンソール表示やOSC通信で外部に出力されるデータは以下の通りです。

* プロトコル名 (例: `"TCP"`, `"HTTP"`)
* パケット長
* パケット番号
* 送信元IP / 宛先IP
* タイムスタンプ
* ポート番号
