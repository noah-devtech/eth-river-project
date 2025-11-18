from ..application_layer import dns_handler, default_handler, http_handler, tls_handler
from utils import format_output, get_nested_attr

# 宛先ポート番号と担当ハンドラーの対応表
APPLICATION_HANDLERS = {
    "dns": dns_handler,  # DNS over TCP
    "http": http_handler, # HTTP over TCP
    "tls": tls_handler
}
# TODO 再送や並び替えの処理の実装

def process(packet, layers, context):
    """
    レイヤー4 (TCP) の処理。
    """
    if not layers or layers[0].layer_name != "tcp":
        print("This is not TCP packet")
        return

    tcp_layer = layers.pop(0)

    context["source_port"] = get_nested_attr(tcp_layer, "srcport")
    context["dest_port"] = get_nested_attr(tcp_layer, "dstport")
    protocol = None
    details = ""

    # 3-way handshake: SYN, SYN+ACK, ACK
    syn = bool(get_nested_attr(tcp_layer, "flags_syn") == "True")
    ack = bool(get_nested_attr(tcp_layer, "flags_ack") == "True")
    fin = bool(get_nested_attr(tcp_layer, "flags_fin") == "True")
    rst = bool(get_nested_attr(tcp_layer, "flags_rst") == "True")

    if syn and not ack:
        protocol = "TCP-SYN"
        details = "3-way Handshake: SYN (Connection Request)"
    elif syn and ack:
        protocol = "TCP-SYN-ACK"
        details = "3-way Handshake: SYN+ACK (Connection Acknowledgment)"
    # TODO 状態管理を実装したら有効化する
    # elif not syn and ack and not fin and not rst:
    #     protocol = "TCP-ACK"
    #     details = "3-way Handshake: ACK (Connection Established)"
    # 4-way handshake: FIN, FIN+ACK, ACK
    elif fin and not ack:
        protocol = "TCP-FIN"
        details = "4-way Handshake: FIN (Connection End Request)"
    elif fin and ack:
        protocol = "TCP-FIN-ACK"
    elif fin and ack:
        protocol = "TCP-FIN-ACK"
        details = "4-way Handshake: FIN+ACK (Connection End Acknowledgment)"
    # TODO 状態管理を実装したら有効化する
    # elif not syn and ack and not fin and not rst:
    #     protocol = "TCP-ACK"
    #     details = "4-way Handshake: ACK (Connection End Confirmed)"
    # RST (Reset)
    elif rst:
        protocol = "TCP-RST"
        details = "Connection Reset (RST)"
    if protocol is not None:
        # この処理はアプリケーション層がないので、パケット長と制御名だけ送ればOK
        format_output(context, protocol, details)
        return

    # TCPセグメントの再構築結果の 'DATA' レイヤーをスキップする
    if layers and get_nested_attr(layers[0], "layer_name") == "DATA":
        context["length"] = packet.DATA.tcp_reassembled_length # TCP再構築後のデータ長に更新
        context["segments"] = str(packet.DATA.tcp_segments)  # セグメント数を追加
        layers.pop(0)  # 'DATA' レイヤーをリストから削除

    if len(layers) == 0:
        # 純粋なACKパケットなど、アプリケーション層が存在しない場合
        # データを含まないTCPのACKパケット(Pure ACK)
        # 今は3-way/4-wayハンドシェイクのACKについてもここで処理しているが、将来的には状態管理を実装して分ける予定
        if get_nested_attr(tcp_layer, "len") == "0":
            protocol = "TCP-ACK"
            details = "Pure ACK Packet"
            format_output(context, protocol, details)
        return

    # アプリケーション層のレイヤー名を取得
    app_layer_name = get_nested_attr(layers[0], "layer_name")

    # ハンドラに振り分け
    handler = APPLICATION_HANDLERS.get(app_layer_name, default_handler)

    # 処理をアプリケーション層に渡す
    handler.process(packet, layers, context)
