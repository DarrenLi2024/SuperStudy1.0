package com.example.sys;

import com.example.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码接口
 * 生成验证码图片并将验证码值存入 Redis（5分钟过期）
 */
@RestController
@RequestMapping("/api/v1/captcha")
public class CaptchaController {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL = 300; // 5分钟
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/generate")
    public ResponseResult<Map<String, String>> generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = generateCode();
        String imageBase64 = generateCaptchaImage(code);

        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + captchaId, code, CAPTCHA_TTL, TimeUnit.SECONDS);

        Map<String, String> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("captchaImage", "data:image/png;base64," + imageBase64);
        return ResponseResult.success(data);
    }

    /**
     * 校验验证码（内部使用，由登录接口调用）
     */
    public boolean validate(String captchaId, String inputCode) {
        if (captchaId == null || inputCode == null) return false;
        String stored = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + captchaId);
        if (stored != null && stored.equalsIgnoreCase(inputCode.trim())) {
            redisTemplate.delete(CAPTCHA_KEY_PREFIX + captchaId); // 一次性使用
            return true;
        }
        return false;
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String code) {
        int width = 120, height = 44;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(240, 242, 245));
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                    RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
            g.fillRect(RANDOM.nextInt(width), RANDOM.nextInt(height), 1, 1);
        }

        // 文字
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(150)));
            double rotate = (RANDOM.nextDouble() - 0.5) * 0.4;
            g.rotate(rotate, 20 + i * 24, 28 + (RANDOM.nextDouble() * 6 - 3));
            g.drawString(String.valueOf(code.charAt(i)), 14 + i * 24, 32);
            g.rotate(-rotate, 20 + i * 24, 28 + (RANDOM.nextDouble() * 6 - 3));
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }
}
