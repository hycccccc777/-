package com.ruoyi.campus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.controller.ApiProductController;
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.domain.CtProductCreateBody;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.service.ICtProductService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品发布审核。
 *
 * <p>覆盖的是"审核这道闸门真的拦得住"：待审核商品不能被公开列表翻出来、
 * 通过后才上架、驳回要留原因且卖家修改后能重新提交、待审核期间不能绕过审核自行上下架。
 */
@DisplayName("商品发布审核")
class ProductAuditTest extends CampusTestSupport
{
    private static final Long SELLER = 2016L;
    private static final Long ADMIN = 1L;

    @Autowired
    private ICtProductService productService;

    @Autowired
    private ApiProductController productController;

    @AfterEach
    void cleanupProducts()
    {
        jdbc.update("delete from ct_product_image where product_id in (select product_id from ct_product where seller_id = ?)", SELLER);
        jdbc.update("delete from ct_product where seller_id = ?", SELLER);
    }

    @Test
    @DisplayName("开启审核：新商品为待审核，且不出现在公开列表")
    void 新商品待审核且不对外展示()
    {
        loginAs(SELLER);
        try
        {
            Long id = publish();
            assertEquals("0", statusOf(id), "发布后应为待审核");
            assertFalse(publicListContains(id), "待审核商品不应出现在公开列表");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("审核通过后上架并出现在公开列表")
    void 审核通过后上架()
    {
        loginAs(SELLER);
        Long id = publish();
        loginAs(ADMIN);
        try
        {
            assertEquals(1, productService.auditProduct(id, true, null));
            assertEquals("1", statusOf(id), "通过后应为已上架");
            assertTrue(publicListContains(id), "通过后应出现在公开列表");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("驳回记录原因，卖家修改后重新进入待审核")
    void 驳回后修改可重新提交()
    {
        loginAs(SELLER);
        Long id = publish();
        loginAs(ADMIN);
        productService.auditProduct(id, false, "图片与描述不符");
        assertEquals("3", statusOf(id), "驳回后应为审核驳回");
        assertEquals("图片与描述不符", auditRemarkOf(id), "应记录驳回原因");

        loginAs(SELLER);
        try
        {
            CtProduct patch = new CtProduct();
            patch.setTitle("修改后的商品标题");
            productService.updateMyProduct(SELLER, id, patch);
            assertEquals("0", statusOf(id), "修改后应重新进入待审核");
            assertEquals("", auditRemarkOf(id), "重新提交应清除旧的驳回原因");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("待审核商品不能自行上下架（绕过审核）")
    void 待审核不能自行上下架()
    {
        loginAs(SELLER);
        try
        {
            Long id = publish();
            ApiException e = assertThrows(ApiException.class,
                    () -> productService.updateMyProductStatus(SELLER, id, "1"));
            assertEquals(409, e.getCode());
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("重复审核被拒绝（不在待审核状态）")
    void 重复审核被拒绝()
    {
        loginAs(SELLER);
        Long id = publish();
        loginAs(ADMIN);
        try
        {
            productService.auditProduct(id, true, null);
            ApiException e = assertThrows(ApiException.class,
                    () -> productService.auditProduct(id, false, "再驳一次"));
            assertEquals(409, e.getCode());
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("公开列表忽略调用方传入的 status，只返回已上架")
    void 公开列表不能按状态翻出待审核商品()
    {
        loginAs(SELLER);
        Long id = publish();
        logout();

        CtProductVo query = new CtProductVo();
        query.setStatus("0"); // 试图直接查待审核
        Map<String, Object> body = productController.list(query, 1, 100).getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        @SuppressWarnings("unchecked")
        List<CtProductVo> list = (List<CtProductVo>) data.get("list");
        boolean leaked = list.stream().anyMatch(p -> id.equals(p.getProductId()));
        assertFalse(leaked, "公开列表不应被 status=0 翻出待审核商品");
    }

    private Long publish()
    {
        CtProductCreateBody body = new CtProductCreateBody();
        body.setTitle("审核测试商品标题");
        body.setCategoryId(1L);
        body.setPrice(new BigDecimal("9.90"));
        body.setCondition("nine");
        body.setDescription("这是一段用于审核测试的商品描述，长度足够。");
        body.setLocation("图书馆南门");
        Map<String, Object> data = productService.publishProduct(body);
        assertNotNull(data.get("id"));
        return Long.valueOf((String) data.get("id"));
    }

    private String auditRemarkOf(Long productId)
    {
        String remark = jdbc.queryForObject("select audit_remark from ct_product where product_id = ?", String.class, productId);
        return remark == null ? "" : remark;
    }

    private boolean publicListContains(Long productId)
    {
        CtProductVo query = new CtProductVo();
        Map<String, Object> body = productController.list(query, 1, 100).getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        @SuppressWarnings("unchecked")
        List<CtProductVo> list = (List<CtProductVo>) data.get("list");
        return list.stream().anyMatch(p -> productId.equals(p.getProductId()));
    }
}
