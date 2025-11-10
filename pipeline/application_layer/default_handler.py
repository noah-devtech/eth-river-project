# pipeline/transport_layer/application_layer/default_handler.py
from utils import format_output,has_nested_attr, get_nested_attr


def process(packet, layers, context):
    """
    未対応のプロトコルやポートの処理。ここでパイプラインは終了。
    """
    if not len(layers) == 0 and has_nested_attr(layers[0],"layer_name"):
        protocol = get_nested_attr(layers[0], "layer_name")
        details = f"Unsupported Protocol"
        format_output(context, protocol, details)
    protocol = "Other"
    details = f"Unsupported Protocol"

    format_output(context, protocol, details)
