from fastapi import FastAPI, Form, Request
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

app = FastAPI()

# 静的ファイルとテンプレートの設定
app.mount("../static", StaticFiles(directory="static"), name="static")
templates = Jinja2Templates(directory="../templates")


@app.post("/auth", response_class=HTMLResponse)
async def login_process(
    request: Request,
    username: str = Form(...),
    switch_url: str = Form(...),
    redirect: str = Form(...),
):
    return templates.TemplateResponse(
        "post.html",
        {
            "request": request,
            "switch_url": switch_url,
            "redirect": redirect,
            "username": username,
        },
    )
