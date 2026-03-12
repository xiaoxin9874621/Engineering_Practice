"""
Flask OCR 微服务主入口
提供 RESTful API 供 Spring Boot 后端调用
"""
import logging
import os
import tempfile

from flask import Flask, request, jsonify
from flask_cors import CORS

from image_processor import preprocess_bytes
from ocr_engine import recognize_by_region

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# 最大图片大小：10MB
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024


@app.route('/api/health', methods=['GET'])
def health():
    """健康检查接口"""
    return jsonify({"code": 200, "message": "OCR service is running"})


@app.route('/api/ocr/recognize', methods=['POST'])
def recognize():
    """
    手写文字识别接口
    请求：multipart/form-data, 字段 'image'（图片文件）
    可选：regions（JSON字符串，题号-区域映射）
    响应：{
        "code": 200,
        "data": {
            "results": [{"question_no": 1, "text": "xxx", "confidence": 0.98}]
        }
    }
    """
    if 'image' not in request.files:
        return jsonify({"code": 400, "message": "缺少图片文件（字段名：image）"}), 400

    image_file = request.files['image']
    if image_file.filename == '':
        return jsonify({"code": 400, "message": "图片文件为空"}), 400

    # 解析区域信息（可选）
    regions = None
    regions_json = request.form.get('regions')
    if regions_json:
        try:
            import json
            regions = json.loads(regions_json)
        except Exception:
            logger.warning("regions 参数解析失败，使用自动分题模式")

    try:
        image_bytes = image_file.read()

        # 图像预处理
        processed_image = preprocess_bytes(image_bytes)

        # OCR 识别
        ocr_result = recognize_by_region(processed_image, regions)

        # 格式化返回
        results = [
            {
                "question_no": q_no,
                "text": text,
                "confidence": 0.95  # 按区域识别时无单独置信度，用平均值
            }
            for q_no, text in ocr_result.items()
        ]

        return jsonify({
            "code": 200,
            "data": {"results": results},
            "message": "识别成功"
        })

    except Exception as e:
        logger.error(f"OCR识别异常: {e}", exc_info=True)
        return jsonify({"code": 500, "message": f"识别失败：{str(e)}"}), 500


@app.route('/api/ocr/preprocess', methods=['POST'])
def preprocess():
    """
    图像预处理接口（返回预处理后的图片，用于调试）
    """
    if 'image' not in request.files:
        return jsonify({"code": 400, "message": "缺少图片文件"}), 400
    try:
        import cv2
        image_bytes = request.files['image'].read()
        processed = preprocess_bytes(image_bytes)
        _, encoded = cv2.imencode('.jpg', processed)
        from flask import send_file
        import io
        return send_file(
            io.BytesIO(encoded.tobytes()),
            mimetype='image/jpeg',
            as_attachment=False,
            download_name='preprocessed.jpg'
        )
    except Exception as e:
        return jsonify({"code": 500, "message": str(e)}), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    debug = os.environ.get('DEBUG', 'false').lower() == 'true'
    logger.info(f"OCR 服务启动在端口 {port}，debug={debug}")
    app.run(host='0.0.0.0', port=port, debug=debug)
