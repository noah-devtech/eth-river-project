from utils import format_output, get_nested_attr

from ..application_layer import dns_handler, http_handler, tls_handler

# 宛先ポート番号と担当ハンドラーの対応表
APPLICATION_HANDLERS = {
    "dns": dns_handler,  # DNS over TCP
    "http": http_handler,  # HTTP over TCP
    "tls": tls_handler,
}
# TODO 再送や並び替えの処理の実装


def process(packet, layers, context):
    """
    レイヤー4 (TCP) の処理。
    """
    if not layers or get_nested_attr(layers[0], "layer_name") != "tcp":
        print("This is not TCP packet")
        return

    tcp_layer = layers.pop(0)

    context["source_port"] = get_nested_attr(tcp_layer, "srcport")
    context["dest_port"] = get_nested_attr(tcp_layer, "dstport")
    protocol = None

    flags = get_nested_attr(tcp_layer, "flags_tree")
    if flags:
        syn = get_nested_attr(flags, "syn") == "1"
        fin = get_nested_attr(flags, "fin") == "1"
        ack = get_nested_attr(flags, "ack") == "1"

        # SYNのみ (接続開始要求)
        if syn and not ack:
            format_output(context, "TCP-SYN")
            return
        # FIN (切断要求)
        if fin:
            format_output(context, "TCP-FIN")
            return

    if protocol is not None:
        format_output(context, protocol)
        return

    if layers:
        app_layer_name = get_nested_attr(layers[0], "layer_name")
        if app_layer_name in APPLICATION_HANDLERS:
            APPLICATION_HANDLERS[app_layer_name].process(packet, layers, context)
            return

    payload_len = get_nested_attr(tcp_layer, "len")

    if payload_len and int(payload_len) > 0:
        # データが含まれているが、プロトコル不明の場合は DATA とする
        format_output(context, "DATA")
    else:
        # データがない (純粋なACKなど)
        format_output(context, "TCP")
