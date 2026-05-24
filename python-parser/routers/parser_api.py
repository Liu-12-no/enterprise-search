from fastapi import APIRouter,UploadFile,File
from services.pdf_service import PDFService

#api路由器
router = APIRouter(prefix="/api/parse", tags=["文档解析模块"])

'''
接口方法：接收外部传来的 PDF 文件流并分发
    
    参数解释：
        - file (UploadFile): 声明接收一个上传的文件对象。
        - = File(...): 告诉 FastAPI 这个参数必须通过 HTTP form-data 表单中的 file 字段传过来（...表示必填项）。
        对标 Java：@RequestParam("file") MultipartFile file
'''
@router.post("/pdf")
async def parse_def(file: UploadFile = File(...)):
    try:
        # 第一步：把用户上传的文件流异步读取到内存中，变成二进制字节 (bytes)
        # 加上 await 是为了防止大文件读取时卡死整个服务器，类似于 Java 的非阻塞 IO (NIO)
            file_bytes = await file.read()

            result = await PDFService.parse_pdf_core(file_bytes, file.filename)

            return {
                "status" : "success",
                "data"  : result
            }
    except Exception as e:
        return{
            "status" : "error",
            "message" : f"PDF解析接口发生异常: {str(e)}"
        }


