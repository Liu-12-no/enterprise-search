import io
import fitz  # PyMuPDF，负责极速读取文本和图片判断
import pdfplumber # 负责精准拆解复杂表格

class PDFService:

    # 声明这是一个静态方法
    '''
    负责处理PDF字节流，提取文字和表格
        - file_bytes:文件的二进制流
        - filename:文件名
    '''
    @staticmethod
    async def parse_pdf_core(file_bytes: bytes, filename: str) -> dict:

        # 初始化返回给java的标准结果模板
        parsed_result = {
            "filename": filename,
            "total_pages": 0,
            "content": []
        }

        # 使用PyMuPDF加在文件流
        doc_fitz = fitz.open(stream=file_bytes, filetype="pdf")

        # 统计总页数并塞进返回结果
        parsed_result["total_pages"] = len(doc_fitz)

        '''
             使用pdfplumber再次加载文件流
             io.BytesIO 把内存里的 bytes 伪装成一个文件
             with 语句对标 Java 的 try-with-resources，执行完会自动关闭文件流，防止内存泄漏
        '''
        with pdfplumber.open(io.BytesIO(file_bytes)) as doc_plumber:

            # 开启循环
            for page_num in range(len(doc_fitz)):

                # 初始化当前页的数据载体
                page_info = {
                    "page": page_num + 1,       # 页码从1开始
                    "text": "",                 # 存放这一页的普通文本
                    "tables": [],               # 存放这一页的所有表格
                    "type": "text"              # 标记这一页是普通文本页还是扫描图片页
                }

                # 拿到具体的页面对象（快速文本提取、识别图片、判断扫描件）
                # 这个句柄擅长处理“整体信息，比如一眼看出这页有没有图片
                page_fitz = doc_fitz[page_num]

                # pdfplumber 句柄 (负责：深入几何解析、拆解复杂表格)
                # 这个句柄擅长处理“空间结构”，通过量测线条坐标，把表格抠出来。
                page_plumber = doc_plumber.pages[page_num]

                # 提取普通文本，去除首位多余的空格
                raw_text = page_fitz.get_text("text").strip()

                # 获取当前页的所有图片元素
                image_list = page_fitz.get_images(full=True)

                if not raw_text and len(image_list) > 0:
                    # 类型标记为扫描件
                    page_info["type"] = "scanned_image"
                    # 这里先用提示语占位。未来你可以写一个 OcrService 调 PaddleOCR，把这段话替换成真正的识别结果！
                    page_info["text"] = "[系统提示：此页为纯扫描件，后续将交由 OCR 视觉模型进行文字提取...]"
                else:
                    # 如果不是扫描件，就把正常提取到的文字献给text
                    page_info["text"] = raw_text

                # 让pdfplumber 扫描当前页面的表格线，返回一个三维数组：[表格1[行[列]], 表格2[行[列]]]
                tables = page_plumber.extract_tables()

                if tables:
                    # 遍历每一个被发现的表格
                    for table in tables:
                        # 重点数据清洗：pdfplumber 遇到合并单元格或者空缺时，会返回 Python 的 None。
                        # None 传给 Java 会变成 null，容易引发空指针异常。
                        # 所以这里用列表推导式，把所有的 None 强行替换成干净的空字符串 ""。
                        cleaned_table = [[cell if cell else "" for cell in row] for row in table]

                        page_info["tables"].append(cleaned_table)

                parsed_result["content"].append(page_info)

        return parsed_result