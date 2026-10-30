package com.ruoyi.campus.storage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 腾讯云 COS 存储（PUT Object，使用 COS v5 签名，直连 HTTP，不依赖 SDK）。
 *
 * <p>对象按 前缀/年/月/日/uuid.ext 组织，避免单目录文件过多。
 * 返回可公网访问的 URL（自定义 CDN 域名优先）。
 *
 * @author campus
 */
public class TencentCosFileStorage implements FileStorage
{
    private final String secretId;
    private final String secretKey;
    private final String bucket;
    private final String region;
    private final String publicBaseUrl;
    private final String keyPrefix;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public TencentCosFileStorage(String secretId, String secretKey, String bucket, String region,
            String publicBaseUrl, String keyPrefix)
    {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.region = region;
        this.publicBaseUrl = publicBaseUrl;
        this.keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "upload" : keyPrefix;
    }

    @Override
    public String store(MultipartFile file)
    {
        String ext = extensionOf(file.getOriginalFilename());
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String key = keyPrefix + "/" + datePath + "/" + IdUtils.fastSimpleUUID() + ext;
        String host = bucket + ".cos." + region + ".myqcloud.com";
        String path = "/" + key;

        long now = System.currentTimeMillis() / 1000;
        String signTime = now + ";" + (now + 600);
        String httpHeaders = "host=" + urlEncode(host);
        String httpString = "put\n" + path + "\n" + "" + "\n" + httpHeaders + "\n";
        String stringToSign = "sha1\n" + signTime + "\n" + sha1Hex(httpString) + "\n";
        String signKey = hmacSha1Hex(secretKey, signTime);
        String signature = hmacSha1Hex(signKey, stringToSign);
        String authorization = "q-sign-algorithm=sha1"
                + "&q-ak=" + secretId
                + "&q-sign-time=" + signTime
                + "&q-key-time=" + signTime
                + "&q-header-list=host"
                + "&q-url-param-list="
                + "&q-signature=" + signature;

        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + encodePath(path)))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authorization)
                    .header("Host", host)
                    .header("Content-Type", contentType(file))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2)
            {
                throw new IllegalStateException("COS 上传失败 HTTP " + response.statusCode() + "：" + response.body());
            }
            String base = (publicBaseUrl == null || publicBaseUrl.isBlank()) ? ("https://" + host) : publicBaseUrl;
            return base.replaceAll("/+$", "") + "/" + key;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("COS 上传被中断", e);
        }
        catch (Exception e)
        {
            if (e instanceof IllegalStateException)
            {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("COS 上传失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String type()
    {
        return "cos";
    }

    private static String extensionOf(String name)
    {
        if (name == null)
        {
            return ".jpg";
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1)
        {
            return ".jpg";
        }
        return name.substring(idx).toLowerCase();
    }

    private static String contentType(MultipartFile file)
    {
        String ct = file.getContentType();
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }

    private static String encodePath(String path)
    {
        StringBuilder sb = new StringBuilder();
        for (String seg : path.split("/", -1))
        {
            if (seg.isEmpty())
            {
                continue;
            }
            sb.append('/').append(urlEncode(seg));
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    private static String urlEncode(String s)
    {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
    }

    private static String sha1Hex(String s)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    private static String hmacSha1Hex(String key, String msg)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }
}
