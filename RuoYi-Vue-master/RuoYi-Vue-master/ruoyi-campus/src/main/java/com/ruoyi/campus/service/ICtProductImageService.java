package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtProductImage;

/**
 * 商品图片 服务层
 * 
 * @author campus
 */
public interface ICtProductImageService
{
    public List<CtProductImage> selectCtProductImageList(CtProductImage ctProductImage);

    public CtProductImage selectCtProductImageById(Long imageId);

    public int insertCtProductImage(CtProductImage ctProductImage);

    public int updateCtProductImage(CtProductImage ctProductImage);

    public int deleteCtProductImageByIds(Long[] imageIds);

    public int deleteCtProductImageByProductId(Long productId);
}
