package com.ruoyi.campus.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.domain.CtProductCreateBody;
import com.ruoyi.campus.domain.CtProductImage;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.mapper.CtProductMapper;
import com.ruoyi.campus.service.ICtProductImageService;
import com.ruoyi.campus.service.ICtProductService;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 商品 服务层实现
 * 
 * @author campus
 */
@Service
public class CtProductServiceImpl implements ICtProductService
{
    @Autowired
    private CtProductMapper ctProductMapper;

    @Autowired
    private ICtProductImageService ctProductImageService;

    @Override
    public List<CtProduct> selectCtProductList(CtProduct ctProduct)
    {
        return ctProductMapper.selectCtProductList(ctProduct);
    }

    @Override
    public CtProduct selectCtProductById(Long productId)
    {
        return ctProductMapper.selectCtProductById(productId);
    }

    @Override
    public List<CtProductVo> selectCtProductVoList(CtProductVo ctProductVo)
    {
        return ctProductMapper.selectCtProductVoList(ctProductVo);
    }

    @Override
    public CtProductVo selectCtProductVoById(Long productId)
    {
        return ctProductMapper.selectCtProductVoById(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishProduct(CtProductCreateBody body)
    {
        if (body == null || StringUtils.isEmpty(StringUtils.trim(body.getTitle())))
        {
            throw new ApiException(400, "商品标题不能为空");
        }
        String title = StringUtils.trim(body.getTitle());
        if (title.length() < 4 || title.length() > 128)
        {
            throw new ApiException(400, "商品标题需要 4～128 个字符");
        }
        if (body.getCategoryId() == null)
        {
            throw new ApiException(400, "请选择商品类目");
        }
        BigDecimal price = body.getPrice();
        if (price == null || price.compareTo(new BigDecimal("0.01")) < 0 || price.compareTo(new BigDecimal("99999")) > 0)
        {
            throw new ApiException(400, "价格需要在 0.01～99999 元之间");
        }

        List<String> images = new ArrayList<>();
        if (body.getImages() != null)
        {
            for (String url : body.getImages())
            {
                if (StringUtils.isNotEmpty(url))
                {
                    images.add(url);
                }
            }
        }
        if (images.size() > 6)
        {
            throw new ApiException(400, "最多上传 6 张图片");
        }

        CtProduct product = new CtProduct();
        product.setTitle(title);
        product.setCategoryId(body.getCategoryId());
        product.setSellerId(SecurityUtils.getUserId());
        product.setSellerType("0");
        product.setPrice(price);
        product.setOriginalPrice(body.getOriginalPrice());
        product.setStock(body.getStock() == null || body.getStock() < 1 ? 1 : body.getStock());
        product.setConditionCode(body.getCondition());
        product.setLocation(body.getLocation());
        product.setTag(body.getTag());
        product.setDescription(body.getDescription());
        product.setCoverUrl(images.isEmpty() ? "" : images.get(0));
        // 演示环境直接上架；如需审核流程，将 status 改为 "0" 走后台审核
        product.setStatus("1");
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setCreateBy(SecurityUtils.getUsername());
        ctProductMapper.insertCtProduct(product);

        int order = 0;
        for (String url : images)
        {
            CtProductImage image = new CtProductImage();
            image.setProductId(product.getProductId());
            image.setImageUrl(url);
            image.setOrderNum(order++);
            ctProductImageService.insertCtProductImage(image);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(product.getProductId()));
        data.put("status", product.getStatus());
        return data;
    }

    @Override
    public boolean deductStock(Long productId, int qty)
    {
        if (qty <= 0)
        {
            throw new ApiException(400, "扣减数量必须大于 0");
        }
        // 影响行数为 0 说明：不存在 / 已下架 / 库存不足（并发下由行锁保证只有一个能成功）
        return ctProductMapper.deductStock(productId, qty) == 1;
    }

    @Override
    public void restoreStock(Long productId, int qty)
    {
        if (qty > 0)
        {
            ctProductMapper.restoreStock(productId, qty);
        }
    }

    @Override
    public List<CtProductVo> selectMyProducts(Long userId, String status)
    {
        CtProductVo query = new CtProductVo();
        query.setSellerId(userId);
        if (StringUtils.isNotEmpty(status))
        {
            query.setStatus(status);
        }
        return ctProductMapper.selectCtProductVoList(query);
    }

    @Override
    public int updateMyProduct(Long userId, Long productId, CtProduct patch)
    {
        CtProduct product = requireOwned(userId, productId);
        if ("4".equals(product.getStatus()))
        {
            throw new ApiException(409, "已售出的商品不可编辑");
        }
        if (StringUtils.isNotEmpty(patch.getTitle()))
        {
            product.setTitle(StringUtils.trim(patch.getTitle()));
        }
        if (patch.getPrice() != null)
        {
            if (patch.getPrice().compareTo(new BigDecimal("0.01")) < 0 || patch.getPrice().compareTo(new BigDecimal("99999")) > 0)
            {
                throw new ApiException(400, "价格需要在 0.01～99999 元之间");
            }
            product.setPrice(patch.getPrice());
        }
        if (patch.getStock() != null)
        {
            product.setStock(patch.getStock() < 0 ? 0 : patch.getStock());
        }
        if (StringUtils.isNotEmpty(patch.getDescription()))
        {
            product.setDescription(patch.getDescription());
        }
        if (StringUtils.isNotEmpty(patch.getLocation()))
        {
            product.setLocation(patch.getLocation());
        }
        if (StringUtils.isNotEmpty(patch.getTag()))
        {
            product.setTag(patch.getTag());
        }
        product.setUpdateBy(SecurityUtils.getUsername());
        return ctProductMapper.updateCtProduct(product);
    }

    @Override
    public int updateMyProductStatus(Long userId, Long productId, String status)
    {
        CtProduct product = requireOwned(userId, productId);
        if (!"1".equals(status) && !"2".equals(status))
        {
            throw new ApiException(400, "目标状态非法，只能上架或下架");
        }
        if ("4".equals(product.getStatus()))
        {
            throw new ApiException(409, "已售出的商品不可上下架");
        }
        product.setStatus(status);
        product.setUpdateBy(SecurityUtils.getUsername());
        return ctProductMapper.updateCtProduct(product);
    }

    @Override
    public int removeMyProduct(Long userId, Long productId)
    {
        CtProduct product = requireOwned(userId, productId);
        if ("4".equals(product.getStatus()))
        {
            throw new ApiException(409, "已售出的商品不可删除");
        }
        return ctProductMapper.deleteCtProductById(product.getProductId());
    }

    private CtProduct requireOwned(Long userId, Long productId)
    {
        CtProduct product = ctProductMapper.selectCtProductById(productId);
        if (product == null || !userId.equals(product.getSellerId()))
        {
            throw new ApiException(404, "商品不存在或无权操作");
        }
        return product;
    }

    @Override
    public int insertCtProduct(CtProduct ctProduct)
    {
        return ctProductMapper.insertCtProduct(ctProduct);
    }

    @Override
    public int updateCtProduct(CtProduct ctProduct)
    {
        return ctProductMapper.updateCtProduct(ctProduct);
    }

    @Override
    public int deleteCtProductByIds(Long[] productIds)
    {
        return ctProductMapper.deleteCtProductByIds(productIds);
    }
}
