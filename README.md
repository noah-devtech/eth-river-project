# Ethernet River: パケット可視化インスタレーション


## Overview

> **Visualizing network traffic flow using OSC protocol.**

**Ethernet River** は、ネットワークパケットの流れを「光の川」としてリアルタイムに可視化するインタラクティブ・インスタレーションです。

* **Purpose:** 目に見えないネットワーク通信の量と流れを、直感的に理解可能な形で物理空間に投影すること。
* **Architecture:**
  * **Capture:** Raspberry Pi上で `Pyshark` を用い、パケットをリアルタイム解析。
  * **Communication:** 解析データをOpenSound Control (OSC) プロトコルで描画用PCへ転送。
  * **Visualization:** Java (Processing) 側で受信データに基づきパーティクルを生成・描画。

## Demo

![eth-river-demo](eth-river.gif)
  
## Tech Stack

<div style="white-space: nowrap;">
  <img src="https://img.shields.io/badge/python-3670A0?logo=python&logoColor=ffdd54" alt="Python" />
  <img src="https://img.shields.io/badge/uv-DE5FE6?logo=python&logoColor=white" alt="uv" />
  <img src="https://img.shields.io/badge/java-%23ED8B00.svg?logo=openjdk&logoColor=white" alt="Java" />
  <img src="https://img.shields.io/badge/Gradle-02303A?logo=Gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/Processing-003366?&logoColor=white" alt="Processing">
  <img src="https://img.shields.io/badge/Linux-FCC624?logo=linux&logoColor=black" alt="Linux" />
  <img src="https://img.shields.io/badge/git-%23F05033.svg?logo=git&logoColor=white" alt="Git" />
  <img src="https://img.shields.io/badge/-GitHub-181717.svg?logo=github" alt="Github" />
  <img src="https://img.shields.io/badge/Wireshark-1679A7?logo=wireshark&logoColor=white" alt="Wireshark" />
  <img src="https://img.shields.io/badge/-Raspberry Pi-C51A4A?logo=Raspberry-Pi" alt="Raspberry-Pi" />
</div>

## Architecture

| マシン       | 役割                     | ディレクトリ                 | 主な技術                          |
| :----------- | :----------------------- | :--------------------------- | :-------------------------------- |
| **マシン 1** | パケット解析・OSC 送信   | [`analyzer/`](analyzer/)     | Python 3.12+, pyshark, python-osc |
| **マシン 2** | 可視化・シミュレーション | [`visualizer/`](visualizer/) | Java 17, Processing 4, oscP5      |
| **マシン 3** |外部Captive Portalサーバー|[]()|docker, Nginx, FastAPI|

※ もしマシン1の処理に余裕がある場合マシン3の機能をマシン1に統合することも可能かもしれません。

### Network Infrastructure

本システムはイベント会場での安定稼働を目的として、以下の機材で構築する予定です。
<!--
* **WAN:** home 5G HR02 or Speed Wi-Fi HOME 5G L13
-->
* **Router:** Yamaha FWX120 (NAPT/L2 Bridge)
* **AP:** CISCO Aironet AIR-AP1832I-Q-K9

## How to use?

### マシン 1（パケット解析: Python）

```sh
cd analyzer
uv run main.py
```

### マシン 2（可視化: Java/Processing）

```sh
cd visualizer
./gradlew build
./gradlew run
```

## Documentation Links

  * [ソフトウェア的なアーキテクチャーについて](docs/ARCHITECTURE.md)
  * [ネットワーク周りのインフラについて](docs/INFRASTRUCTURE.md)
  * [TODOリスト](docs/TODO.md)