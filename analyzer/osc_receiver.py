import argparse

from pythonosc import dispatcher, osc_server


def print_message_handler(address: str, *args: object) -> None:
    """
    受信したOSCメッセージをコンソールに表示するハンドラ
    """
    print(f"[OSC受信] アドレス: {address}")

    # args は受信したデータのタプルです
    if args:
        print("  データ:")
        for i, arg in enumerate(args):
            print(f"    引数 {i}: {arg} (型: {type(arg)})")
    else:
        print("  (データなし)")


def main(ip: str, port: int) -> None:
    # OSCメッセージのアドレスパターンに応じて関数を割り当てる
    disp = dispatcher.Dispatcher()

    # "/*" は、/packet/dns や /packet/http_request など、
    # あらゆるアドレスのメッセージを捕捉します。
    disp.map("/*", print_message_handler)

    # OSCサーバーをセットアップ
    # "0.0.0.0" は、このマシン（N100など）の持つ全てのIPアドレスで待ち受けることを意味します
    server = osc_server.ThreadingOSCUDPServer((ip, port), disp)

    print(f"[*] OSCサーバーを {ip}:{port} で起動しました。")
    print("[*] メッセージの受信を待っています... (Ctrl+C で終了)")

    # サーバーを（別スレッドで）起動
    server.serve_forever()


if __name__ == "__main__":
    # 起動時の引数を設定（デフォルトは 12345 ポート）
    parser = argparse.ArgumentParser()
    parser.add_argument("--ip", default="127.0.0.1", help="待ち受けるIPアドレス")
    parser.add_argument("--port", type=int, default=12345, help="待ち受けるポート番号")
    args = parser.parse_args()

    main(args.ip, args.port)
