# pipeline/transport_layer/udp_handler.py
from ..application_layer import dns_handler, default_handler
from utils import format_output, get_nested_attr

# 宛先ポート番号と担当ハンドラーの対応表
APPLICATION_HANDLERS = {
    "dns": dns_handler,
}


def process(packet, layers, context):
    """
    レイヤー4 (UDP) の処理。
    """
    if not layers or layers[0].layer_name != "udp":
        print("This is not UDP packet")
        return

    udp_layer = layers.pop(0)

    context["source_port"] = get_nested_attr(udp_layer, "srcport")
    context["dest_port"] = get_nested_attr(udp_layer, "dstport")

    handler = APPLICATION_HANDLERS.get(layers[0].layer_name, default_handler)

    handler.process(packet, layers, context)
