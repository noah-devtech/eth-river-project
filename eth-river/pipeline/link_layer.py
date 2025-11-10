# pipeline/link_layer.py
from . import network_layer
from utils import format_output, get_nested_attr


def process(packet, layers, context):
    """
    レイヤー2 (イーサネット) の処理。
    """
    if not layers or layers[0].layer_name != "eth":
        print("link layer : unknown protocol")
        return

    eth_layer = layers.pop(0)  # 先頭のレイヤーを処理してリストから削除

    context["source_mac"] = get_nested_attr(eth_layer, "src")
    context["dest_mac"] = get_nested_attr(eth_layer, "dst")

    # 次のレイヤーの処理を呼び出す
    network_layer.process(packet, layers, context)
