"""
图像预处理模块
对手写作业图片进行去噪、纠偏、增强等预处理操作
"""
import cv2
import numpy as np
from PIL import Image


def preprocess_image(image_path: str) -> np.ndarray:
    """
    完整图像预处理流程：灰度化 → 去噪 → 二值化 → 透视校正
    :param image_path: 图片路径
    :return: 预处理后的 numpy 数组（BGR格式）
    """
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"无法读取图片：{image_path}")

    # 1. 灰度化
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    # 2. 高斯去噪
    denoised = cv2.GaussianBlur(gray, (3, 3), 0)

    # 3. 自适应二值化（手写文字效果更好）
    binary = cv2.adaptiveThreshold(
        denoised, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        11, 2
    )

    # 4. 形态学操作（去除小噪点）
    kernel = np.ones((2, 2), np.uint8)
    cleaned = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)

    # 5. 倾斜校正
    deskewed = deskew(cleaned)

    # 转回BGR供PaddleOCR使用
    result = cv2.cvtColor(deskewed, cv2.COLOR_GRAY2BGR)
    return result


def deskew(binary_image: np.ndarray) -> np.ndarray:
    """
    基于最小外接矩形的倾斜校正
    """
    try:
        coords = np.column_stack(np.where(binary_image < 128))
        if len(coords) < 10:
            return binary_image
        angle = cv2.minAreaRect(coords)[-1]
        if angle < -45:
            angle = -(90 + angle)
        else:
            angle = -angle
        # 角度过大时不处理（可能是旋转图片，不是倾斜）
        if abs(angle) > 15:
            return binary_image
        (h, w) = binary_image.shape[:2]
        center = (w // 2, h // 2)
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        rotated = cv2.warpAffine(
            binary_image, M, (w, h),
            flags=cv2.INTER_CUBIC,
            borderMode=cv2.BORDER_REPLICATE
        )
        return rotated
    except Exception:
        return binary_image


def preprocess_bytes(image_bytes: bytes) -> np.ndarray:
    """从字节数据预处理（用于接收上传图片）"""
    nparr = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("无法解析图片数据")

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    denoised = cv2.GaussianBlur(gray, (3, 3), 0)
    binary = cv2.adaptiveThreshold(
        denoised, 255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY, 11, 2
    )
    kernel = np.ones((2, 2), np.uint8)
    cleaned = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)
    deskewed = deskew(cleaned)
    return cv2.cvtColor(deskewed, cv2.COLOR_GRAY2BGR)
