package com.project3.inventoryservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CloudinaryService {

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    public boolean deleteImage(String publicId) {
        try {
            if (publicId == null || publicId.isEmpty()) return true;
            long timestamp = System.currentTimeMillis() / 1000;
            String toSign = "public_id=" + publicId + "&timestamp=" + timestamp + apiSecret;
            String signature = org.apache.commons.codec.digest.DigestUtils.sha1Hex(toSign);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("public_id", publicId);
            form.add("timestamp", String.valueOf(timestamp));
            form.add("api_key", apiKey);
            form.add("signature", signature);

            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/destroy";
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            restTemplate.postForEntity(url, request, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


