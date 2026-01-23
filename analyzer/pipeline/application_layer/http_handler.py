from utils import format_output, no_higher_layer


def process(packet, layers, context):
    """
    レイヤー7 (HTTP) の処理。
    """
    if not layers or layers[0].layer_name != "http":
        no_higher_layer(layers, "http")
        return

    # http_layer = layers.pop(0)
    format_output(context, "HTTP")
