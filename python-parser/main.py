from fastapi import FastAPI
from routers import parser_api # 把咱们刚写的路由接进来
import uvicorn

# 1. 实例化核心应用对象（微服务的心脏）
app = FastAPI(
    title="企业查询 - Python文档解析微服务",
    description="提供基于 PyMuPDF 和 pdfplumber 的工业级混合文档解析能力",
    version="1.0.0"
)

# 2. 路由注册：把接口全部挂载到心脏上
app.include_router(parser_api.router)

# 3. 留一个最轻量级的心跳检测接口（探针）
@app.get("/ping")
def ping():
    return {"status": "ok", "message": "Python 核心解析微服务已就绪，随时可以传文件！"}


if __name__ == "__main__":
    uvicorn.run("main:app",host="127.0.0.1",port=8000,reload=True)