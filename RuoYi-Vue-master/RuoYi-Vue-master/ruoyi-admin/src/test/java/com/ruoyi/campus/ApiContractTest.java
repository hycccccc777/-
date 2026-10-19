package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;

/**
 * 接口契约与后端实现的一致性测试。
 *
 * <p>作用是把「加了接口但忘了更新契约」和「改了接口路径但前端还在按老路径调」
 * 这类问题拦在 CI 上，而不是等到联调时才发现。
 *
 * <p>契约文件 openapi/campus.openapi.json 由运行中的服务导出，
 * 本测试做的是「反向校验」：扫描所有 /api/v1 控制器方法，
 * 确认它们在契约里都有对应条目，且都写了 @Operation 说明。
 *
 * @author campus
 */
class ApiContractTest
{
    /** 契约快照路径：从 ruoyi-admin 模块目录往上三级是仓库根 */
    private static final Path CONTRACT = Paths.get("..", "..", "..", "openapi", "campus.openapi.json")
            .toAbsolutePath().normalize();

    private static final String SCAN_PACKAGE = "com.ruoyi.campus.controller";

    @Test
    @DisplayName("契约快照文件存在且可解析")
    void contractFileExists() throws IOException
    {
        assertTrue(Files.exists(CONTRACT), "找不到契约文件：" + CONTRACT);
        JsonNode root = new ObjectMapper().readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        assertNotNull(root.get("paths"), "契约缺少 paths");
    }

    @Test
    @DisplayName("每个 /api/v1 接口都写了 @Operation 说明")
    void everyEndpointHasOperation() throws Exception
    {
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Method> entry : scanEndpoints().entrySet())
        {
            if (entry.getValue().getAnnotation(Operation.class) == null)
            {
                missing.add(entry.getKey());
            }
        }
        assertEquals(List.of(), missing, "以下接口缺少 @Operation 注解：\n" + String.join("\n", missing));
    }

    @Test
    @DisplayName("每个 /api/v1 接口都出现在契约快照中")
    void everyEndpointIsInContract() throws Exception
    {
        JsonNode paths = new ObjectMapper().readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8)).get("paths");
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Method> entry : scanEndpoints().entrySet())
        {
            String key = entry.getKey();
            int idx = key.indexOf(' ');
            String method = key.substring(0, idx).toLowerCase();
            String path = key.substring(idx + 1);
            JsonNode item = paths.get(path);
            if (item == null)
            {
                missing.add(key + " —— 契约中无此路径（请重新导出 openapi/campus.openapi.json）");
            }
            else if (item.get(method) == null)
            {
                missing.add(key + " —— 契约中该路径没有此方法");
            }
        }
        assertEquals(List.of(), missing, "以下接口未同步到契约快照：\n" + String.join("\n", missing));
    }

    @Test
    @DisplayName("契约快照里的接口在代码中都存在（防止删了接口忘了删契约）")
    void contractHasNoGhostEndpoints() throws Exception
    {
        JsonNode paths = new ObjectMapper().readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8)).get("paths");
        Map<String, Method> endpoints = scanEndpoints();
        List<String> ghosts = new ArrayList<>();
        paths.fieldNames().forEachRemaining(path ->
        {
            paths.get(path).fieldNames().forEachRemaining(method ->
            {
                if ("parameters".equals(method))
                {
                    return;
                }
                String key = method.toLowerCase() + " " + path;
                if (!endpoints.containsKey(key))
                {
                    ghosts.add(key);
                }
            });
        });
        assertEquals(List.of(), ghosts, "契约中存在已删除的接口，请重新导出：\n" + String.join("\n", ghosts));
    }

    /**
     * 扫描所有 /api/v1 的接口方法。
     *
     * @return key 形如 "get /api/v1/products"，value 为方法对象
     */
    private Map<String, Method> scanEndpoints() throws Exception
    {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Map<String, Method> result = new LinkedHashMap<>();
        for (BeanDefinition def : scanner.findCandidateComponents(SCAN_PACKAGE))
        {
            Class<?> clazz = Class.forName(def.getBeanClassName());
            String base = basePath(clazz);
            if (base == null || !base.startsWith("/api/v1"))
            {
                // 非前端接口（如 /campus/** 的后台接口）不在前端契约范围内
                continue;
            }
            for (Method method : clazz.getDeclaredMethods())
            {
                String verb = httpMethod(method);
                if (verb == null)
                {
                    continue;
                }
                String sub = subPath(method);
                result.put(verb + " " + joinPath(base, sub), method);
            }
        }
        return result;
    }

    private String basePath(Class<?> clazz)
    {
        RequestMapping rm = clazz.getAnnotation(RequestMapping.class);
        return rm == null || rm.value().length == 0 ? "" : rm.value()[0];
    }


    private String httpMethod(Method method)
    {
        if (method.getAnnotation(GetMapping.class) != null)
        {
            return "get";
        }
        if (method.getAnnotation(PostMapping.class) != null)
        {
            return "post";
        }
        if (method.getAnnotation(PutMapping.class) != null)
        {
            return "put";
        }
        if (method.getAnnotation(DeleteMapping.class) != null)
        {
            return "delete";
        }
        if (method.getAnnotation(PatchMapping.class) != null)
        {
            return "patch";
        }
        return null;
    }

    private String subPath(Method method)
    {
        GetMapping g = method.getAnnotation(GetMapping.class);
        if (g != null)
        {
            return g.value().length == 0 ? "" : g.value()[0];
        }
        PostMapping p = method.getAnnotation(PostMapping.class);
        if (p != null)
        {
            return p.value().length == 0 ? "" : p.value()[0];
        }
        PutMapping u = method.getAnnotation(PutMapping.class);
        if (u != null)
        {
            return u.value().length == 0 ? "" : u.value()[0];
        }
        DeleteMapping d = method.getAnnotation(DeleteMapping.class);
        if (d != null)
        {
            return d.value().length == 0 ? "" : d.value()[0];
        }
        PatchMapping pa = method.getAnnotation(PatchMapping.class);
        if (pa != null)
        {
            return pa.value().length == 0 ? "" : pa.value()[0];
        }
        return "";
    }

    private String joinPath(String base, String sub)
    {
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String s = sub.startsWith("/") ? sub : "/" + sub;
        if (s.equals("/"))
        {
            return b;
        }
        return b + s;
    }
}