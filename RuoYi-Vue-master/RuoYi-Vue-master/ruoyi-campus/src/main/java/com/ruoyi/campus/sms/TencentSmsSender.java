package com.ruoyi.campus.sms;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 腾讯云短信发送器（TC3-HMAC-SHA256 签名，直连官方 API，不依赖 SDK）。
 *
 * <p>调用 SendSms 接口（SmsSdkAppId + SignName + TemplateId + 模板参数）。
 * 模板约定：模板里只有一个变量 {@code {1}}，即验证码。
 *
 * <p>凭据全部来自配置（环境变量），不在代码里固化。
 *
 * @author campus
 */
public class TencentSmsSender implements SmsSender
{
    private static final Logger log = LoggerFactory.getLogger(TencentSmsSender.class);

    private static final String HOST = "sms.tencentcloudapi.com";
    private static final String SERVICE = "sms";
    private static final String ACTION = "SendSms";
    private static final String VERSION = "2021-01-11";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final String secretId;
    private final String secretKey;
    private final String sdkAppId;
    private final String signName;
    private final String templateId;
    private final String region;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public TencentSmsSender(String secretId, String secretKey, String sdkAppId, String signName,
            String templateId, String region)
    {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.sdkAppId = sdkAppId;
        this.signName = signName;
        this.templateId = templateId;
        this.region = (region == null || region.isBlank()) ? "ap-guangzhou" : region;
    }

    @Override
    public void sendCode(String phone, String scene, String code)
    {
        String payload = "{"
                + "\"PhoneNumberSet\":[\"+86" + phone + "\"],"
                + "\"SmsSdkAppId\":\"" + sdkAppId + "\","
                + "\"SignName\":\"" + signName + "\","
                + "\"TemplateId\":\"" + templateId + "\","
                + "\"TemplateParamSet\":[\"" + code + "\"]"
                + "}";
        long timestamp = Instant.now().getEpochSecond();
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(timestamp));

        String signedHeaders = "content-type;host;x-tc-action";
        String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n"
                + "host:" + HOST + "\n"
                + "x-tc-action:" + ACTION.toLowerCase();
        String canonicalRequest = "POST" + "\n"
                + "/" + "\n"
                + "" + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + sha256Hex(payload);

        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String stringToSign = "TC3-HMAC-SHA256" + "\n"
                + timestamp + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = HexFormat.of().formatHex(hmacSha256(secretSigning, stringToSign));

        String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + HOST + "/"))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", authorization)
                .header("Content-Type", CONTENT_TYPE)
                .header("Host", HOST)
                .header("X-TC-Action", ACTION)
                .header("X-TC-Version", VERSION)
                .header("X-TC-Timestamp", String.valueOf(timestamp))
                .header("X-TC-Region", region)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        try
        {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200)
            {
                log.error("腾讯云短信 HTTP {}：{}", response.statusCode(), response.body());
                throw new IllegalStateException("短信服务返回异常");
            }
            String body = response.body();
            // 腾讯云在 200 里也可能返回错误码
            if (body.contains("\"Error\""))
            {
                log.error("腾讯云短信发送失败：{}", body);
                throw new IllegalStateException("短信发送失败");
            }
            log.info("短信已提交腾讯云 scene={} phone={}", scene, phone);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("短信发送被中断", e);
        }
        catch (Exception e)
        {
            if (e instanceof IllegalStateException)
            {
                throw (IllegalStateException) e;
            }
            log.error("短信发送失败", e);
            throw new IllegalStateException("短信发送失败", e);
        }
    }

    @Override
    public String provider()
    {
        return "tencent";
    }

    private static String sha256Hex(String s)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String msg)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }
}
