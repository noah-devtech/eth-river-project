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

    if len(layers) == 0:
        print("TCP has no higher layer")
        return
    if get_nested_attr(layers[0],"layer_name") == "DATA":
        # pysharkがリアセンブルしてくれたパケットが乗っていたらここ来る
        # 今のところ上のパケットを処理するためにセグメンテーションされたセグメントは捨てる
        layers.pop(0)
    if len(layers) > 1:
        handler = APPLICATION_HANDLERS.get(
            get_nested_attr(layers[0],"layer_name"),
            default_handler
        )
        handler.process(packet, layers, context)
    else:
        # DATAレイヤーの上に何もないとき
        print("only DATA layer")
        return