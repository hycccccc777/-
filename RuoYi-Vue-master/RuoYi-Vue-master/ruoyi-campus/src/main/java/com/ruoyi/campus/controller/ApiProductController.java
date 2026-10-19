package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtProductCreateBody;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.service.ICtProductService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * ???? /api/v1 ??????????
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
public class ApiProductController
{
    @Autowired
    private ICtProductService productService;

    /**
     * ????б???keyword??categoryId??school??sellerType??condition??minPrice??maxPrice??sort??page??size
     */
    @Anonymous
    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> list(CtProductVo query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "12") int size)
    {
        if (page < 1)
        {
            page = 1;
        }
        if (size < 1 || size > 100)
        {
            size = 12;
        }
        if (StringUtils.isEmpty(query.getStatus()))
        {
            // ???????б????????????
            query.setStatus("1");
        }
        PageHelper.startPage(page, size);
        try
        {
            List<CtProductVo> list = productService.selectCtProductVoList(query);
            PageInfo<CtProductVo> info = new PageInfo<>(list);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", info.getTotal());
            data.put("page", page);
            data.put("size", size);
            data.put("list", info.getList());
            return ApiResp.ok(data);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    /**
     * ???????
     */
    @Anonymous
    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") Long id)
    {
        CtProductVo vo = productService.selectCtProductVoById(id);
        if (vo == null)
        {
            return ApiResp.fail(404, "???????????????");
        }
        return ApiResp.ok(vo);
    }

    /**
     * ???????????????
     */
    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CtProductCreateBody body)
    {
        // 说明：/api/v1/products 的 GET 用 @Anonymous 放行后，Spring Security 的路径匹配
        // 不区分方法，会连带放行同路径的 POST，因此这里显式校验登录态。
        try
        {
            SecurityUtils.getLoginUser();
        }
        catch (Exception e)
        {
            return ApiResp.fail(401, "请先登录后再发布商品");
        }
        try
        {
            return ApiResp.ok(productService.publishProduct(body));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}
