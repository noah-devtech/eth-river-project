# pipeline/application_layer/tls_handler.py
from utils import format_output, get_nested_attr, no_higher_layer


def process(packet, layers, context):
    """
    レイヤー7 (TLS/SSL) の処理。
    """

    # まず 'tls' レイヤー自体を取得
    tls_layer = get_nested_attr(packet, "tls")
    if not tls_layer:
        no_higher_layer(layers, "tls")
        return

    sni_domain = get_nested_attr(tls_layer, "handshake_extensions_server_name")

    if sni_domain:
        details = f"HTTPS (TLS) SNI: {sni_domain}"
        format_output(context, "TLS-Hello", details)
    else:
        details = "TLS Encrypted Data or No SNI"
        format_output(context, "TLS", details)
