package com.homework.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * OCR服务调用（调用独立的PaddleOCR Flask微服务）
 */
@Slf4j
@Service
public class OcrService {

    @Value("${ocr.service.url}")
    private String ocrServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 识别图片中的手写文字
     * @param imageFile 上传的图片文件
     * @return Map<题号, 识别文本>
     */
    public Map<Integer, String> recognize(MultipartFile imageFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };
            body.add("image", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ocrServiceUrl + "/api/ocr/recognize",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseOcrResult(response.getBody());
            }
        } catch (IOException e) {
            log.error("OCR识别失败：读取文件异常", e);
        } catch (Exception e) {
            log.error("OCR服务调用失败：{}", e.getMessage());
        }
        // OCR服务不可用时返回空结果（不阻断流程）
        return new HashMap<>();
    }

    /**
     * 解析OCR服务返回结果
     * 期望格式：{"code":200,"data":{"results":[{"question_no":1,"text":"答案内容","confidence":0.98}]}}
     */
    private Map<Integer, String> parseOcrResult(String responseBody) {
        Map<Integer, String> result = new HashMap<>();
        try {
            JSONObject resp = JSON.parseObject(responseBody);
            if (resp.getInteger("code") == 200) {
                var dataArr = resp.getJSONObject("data").getJSONArray("results");
                if (dataArr != null) {
                    for (int i = 0; i < dataArr.size(); i++) {
                        JSONObject item = dataArr.getJSONObject(i);
                        result.put(item.getInteger("question_no"), item.getString("text"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析OCR结果失败：{}", e.getMessage());
        }
        return result;
    }

    /**
     * 检查OCR服务健康状态
     */
    public boolean isOcrServiceAvailable() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(ocrServiceUrl + "/api/health", String.class);
            return resp.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
