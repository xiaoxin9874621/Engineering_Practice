"""
PaddleOCR 识别引擎封装
支持中文、英文手写文字识别，按题号区域分割识别结果
"""
import json
import logging
import time
from typing import Dict, List, Optional

import numpy as np

logger = logging.getLogger(__name__)

# 延迟初始化，避免导入时因环境问题崩溃
_ocr_engine = None


def get_ocr_engine():
    global _ocr_engine
    if _ocr_engine is None:
        try:
            from paddleocr import PaddleOCR
            _ocr_engine = PaddleOCR(
                use_angle_cls=True,    # 启用方向分类（处理倒置文字）
                lang='ch',             # 中文模式（同时支持英文）
                use_gpu=False,         # 无GPU环境设为False，有GPU改为True
                show_log=False,
                enable_mkldnn=True,    # CPU推理加速
            )
            logger.info("PaddleOCR 引擎初始化成功")
        except ImportError:
            logger.warning("PaddleOCR 未安装，使用模拟识别模式")
            _ocr_engine = MockOcrEngine()
    return _ocr_engine


class MockOcrEngine:
    """开发调试用模拟OCR引擎（PaddleOCR未安装时使用）"""
    def ocr(self, image, cls=True):
        # 返回模拟结果
        return [[[
            [[10, 10], [100, 10], [100, 30], [10, 30]],
            ("模拟识别结果", 0.95)
        ]]]


def recognize_image(image_array: np.ndarray) -> List[Dict]:
    """
    识别图片中的所有文字
    :param image_array: BGR格式的numpy数组
    :return: 识别结果列表 [{"text": str, "confidence": float, "box": list}]
    """
    engine = get_ocr_engine()
    start_time = time.time()

    try:
        result = engine.ocr(image_array, cls=True)
        elapsed_ms = int((time.time() - start_time) * 1000)
        logger.info(f"OCR识别完成，耗时 {elapsed_ms}ms")

        records = []
        if result and result[0]:
            for line in result[0]:
                if line is None:
                    continue
                box, (text, confidence) = line
                records.append({
                    "text": text,
                    "confidence": round(float(confidence), 4),
                    "box": [[int(p[0]), int(p[1])] for p in box],
                    "process_time_ms": elapsed_ms
                })
        return records
    except Exception as e:
        logger.error(f"OCR识别失败: {e}")
        return []


def recognize_by_region(image_array: np.ndarray, regions: Optional[List[Dict]] = None) -> Dict[int, str]:
    """
    按题号区域识别（如提供了区域信息则分区识别，否则按行自动分题）
    :param image_array: 图片数组
    :param regions: 区域列表 [{"question_no": 1, "y1": 0, "y2": 100}]
    :return: {题号: 识别文本}
    """
    all_texts = recognize_image(image_array)

    if not all_texts:
        return {}

    if regions:
        # 按指定区域分配文字
        return assign_texts_to_regions(all_texts, regions, image_array.shape[0])
    else:
        # 自动按行顺序分配（默认每段文字对应一道题）
        return auto_assign_texts(all_texts)


def assign_texts_to_regions(texts: List[Dict], regions: List[Dict], image_height: int) -> Dict[int, str]:
    """将识别结果按区域分配到对应题号"""
    result = {}
    for region in regions:
        q_no = region["question_no"]
        y1 = region.get("y1", 0)
        y2 = region.get("y2", image_height)
        region_texts = []
        for t in texts:
            # 取文字框中心y坐标
            center_y = sum(p[1] for p in t["box"]) / 4
            if y1 <= center_y <= y2:
                region_texts.append(t["text"])
        result[q_no] = " ".join(region_texts)
    return result


def auto_assign_texts(texts: List[Dict]) -> Dict[int, str]:
    """
    自动分题：按y坐标排序，每个独立文字块作为一道题答案
    适用于题号+答案换行的格式
    """
    sorted_texts = sorted(texts, key=lambda t: sum(p[1] for p in t["box"]) / 4)
    result = {}
    q_no = 1
    for item in sorted_texts:
        result[q_no] = item["text"]
        q_no += 1
    return result
