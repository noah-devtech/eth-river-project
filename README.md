# eth_river_vis

## 概要

eth_river_vis は、受信したネットワークパケットを「流れる粒子」として可視化する小さな Java/Processing スケッチです。UDP ポートで OSC（Open Sound Control）メッセージを受信し、そのメッセージに含まれるパケットのメタデータに基づいて粒子の色や大きさを決定し、生成します。想定されている OSC 送信元は外部の Python スクリプト（コード内コメントで「pyshark (main.py)」と記載）ですが、このリポジトリには含まれていません。

## 技術スタック / 依存関係

-   言語: Java
-   ビルドツール: Gradle（Wrapper 同梱）＋ Kotlin DSL ビルドスクリプト
    -   Gradle Wrapper: 8.14（gradle/wrapper/gradle-wrapper.properties を参照）
-   可視化フレームワーク: Processing Core 4.3.4（org.processing:core）
-   OSC ライブラリ: oscP5 0.9.8（de.sojamo:oscp5）
-   テストフレームワーク: JUnit 5（依存関係はあるが、現状テストは未収録）

## エントリポイント

-   メインクラス: Main
-   起動方法: Processing の PApplet エントリポイント（PApplet.main("Main")）
-   ソース: src/main/java/Main.java

## 実行時の挙動

-   ウィンドウ（800x600）を開き、下方向に漂う粒子を描画します。
-   UDP ポート 12345 で受信する OSC メッセージを待ち受けます。
-   受信した OSC メッセージから（protocol, length, details, source IP, destination IP）を取り出してデバッグ表示し、プロトコル種別に応じた色、パケット長に応じた大きさの粒子を生成します。

## プロジェクト構成

-   build.gradle.kts — Gradle ビルド（Kotlin DSL）。Processing と oscP5 の依存関係、および JUnit プラットフォームを宣言。
-   settings.gradle.kts — プロジェクト名の設定。
-   gradlew / gradlew.bat — Gradle Wrapper ランチャ（Unix/Windows）。
-   gradle/wrapper/\* — Gradle Wrapper の設定とバイナリ。
-   src/main/java/Main.java — Processing スケッチ本体および OSC リスナー/可視化ロジック。

## 要件

-   Java Development Kit（JDK）:
    -   推奨: JDK 17 以上（Gradle 8.x は 17/21 と相性良好）。ツールチェーンの問題が出る場合は、まず JDK 17 を試してください。
-   依存関係の取得のためのインターネット接続（Maven Central, jogamp, clojars）。
-   このスケッチに対して UDP 12345 に互換な OSC メッセージを送信する送信元。
    -   コードコメントでは pyshark を用いた Python スクリプトに言及していますが、本リポジトリには含まれていません。

## セットアップ

1. 対応する JDK をインストールし、PATH に通っていることを確認します（java -version）。
2. 本リポジトリをクローンします。
3. ビルドには Gradle Wrapper を使用してください（ローカルに Gradle を入れる必要はありません）。

## ビルドと実行

ビルド（コンパイルとテスト実行）:

-   Windows: .\gradlew.bat build
-   macOS/Linux: ./gradlew build

アプリの実行:

-   Gradle の application プラグインは未設定のため、現時点では gradlew run タスクはありません。
-   推奨: IDE（IntelliJ IDEA など）でプロジェクトを開き、Main クラスを直接実行してください。
    -   Main は Processing スケッチであり、実行すると PApplet のウィンドウが起動します。

## アプリへの OSC データ入力

-   既定では UDP ポート 12345 をリッスンします。
-   少なくとも次の 5 つの引数をこの順番で持つ OSC メッセージを想定しています:
    1. protocol（String）, 2) length（int）, 3) details（String）, 4) source IP（String）, 5) destination IP（String）
-   受信メッセージから取得したアドレス（lastAddress）を表示しますが、コード上で特定のアドレスパターンを強制はしていません。

## 環境変数と設定

-   現状、このコードで定義・使用している環境変数はありません。
-   OSC の受信ポートは Main.java 内でハードコードされています（listenPort = 12345）。
-   TODO: 受信ポート（および色/サイズのマッピング等）を、環境変数・CLI 引数・設定ファイルなどで変更可能にする。

## Gradle スクリプトと便利なタスク

-   gradlew / gradlew.bat — ローカルに Gradle を入れずに呼び出せる Wrapper スクリプト。
-   主なタスク:
    -   build — コンパイルおよびテスト実行。
    -   test — ユニットテストの実行（現状テストは未収録）。
    -   clean — ビルド成果物の削除。

## テスト

-   本プロジェクトは JUnit 5 の依存関係を含みますが、現状テストソースはありません。
-   TODO: src/test/java 配下にテストを追加し、以下で実行:
    -   Windows: .\gradlew.bat test
    -   macOS/Linux: ./gradlew test

## 開発メモ

-   Processing 連携: Processing Core を通常の Java 依存として利用しており、Processing IDE は不要です。一般的な Java IDE からスケッチを実行できます。
-   OSC メッセージ: oscP5 により処理されます。メッセージ解析中のエラーはコンソールにログ出力されます。

## トラブルシューティング

-   何も表示されない／すぐ終了する: Main クラス（Processing の PApplet エントリ）を起動しているか確認してください。存在しない Gradle の run タスクを実行しようとしていないか注意。
-   粒子が出ない: 同一マシンの UDP 12345 に向けて OSC 送信が行われているか、またファイアウォールで当該ポートの UDP 受信が許可されているか確認してください。
-   依存解決に失敗する: ネットワーク接続状況と JDK の互換性を確認してください（JDK 17 を試す）。

## ライセンス

-   本リポジトリには LICENSE ファイルが存在しません。
-   TODO: ライセンスファイルを追加し、ここに採用ライセンスを明記してください。

## 謝辞

-   Processing — The Processing Foundation
-   oscP5 — Andreas Schlegel（de.sojamo:oscp5）
