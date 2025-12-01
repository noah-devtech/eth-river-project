from utils import format_output, get_nested_attr, no_higher_layer


def process(packet, layers, context):
    """
    レイヤー7 (HTTP) の処理。
    """
    if not layers or layers[0].layer_name != "http":
        no_higher_layer(layers, "http")
        return

    http_layer = layers.pop(0)
    http_url = http_layer.get_field("request_full_uri")
    if get_nested_attr(http_layer,"request") == "True":
        details = f"url: {http_url}"
        """
        # HTTPリクエストメソッドで場合分け
        request_method = http_layer.request_method
        if request_method == "GET":
            # GET
        elif request_method == "POST":
            # POST
        elif request_method == "PUT":
            # PUT
        elif request_method == "PATCH":
            # PATCH
        elif request_method == "DELETE":
            # DELETE
        else:
            request_method = "N/A"
    """
    else:
        # HTTPレスポンスの場合
        http_context = get_nested_attr(http_layer,"")
        details = f"url: {http_url} context: {http_context}"

    # ステータスコード
    # 2xx
    """
    if 200 <= response.status_code < 300:
    data = response.json()
    process_data(data)
    """
    # 4xx
    """
    elif 400 <= response.status_code < 500:
    # 優先度2A: クライアントエラー時の処理
    if response.status_code == 404:
        print("エラー: 対象のデータが見つかりませんでした。")
    elif response.status_code == 401:
        print("エラー: 認証が必要です。")
    else:
        print(f"クライアントエラーが発生しました: {response.status_code}")
    """
    # 5xx
    """
    elif 500 <= response.status_code < 600:
    # 優先度2B: サーバーエラー時の処理
    print(f"サーバーエラーが発生しました: {response.status_code}。時間をおいて再試行してください。")
    """
    """
    else:
    # 優先度3など、上記以外（3xxや1xx）
    # （ライブラリがリダイレクトを処理した後は、通常ここには来ない）
    print(f"予期せぬステータスコードです: {response.status_code}")
    """

    format_output(context, "HTTP", details)
