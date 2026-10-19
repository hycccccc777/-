package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtProductImage;

/**
 * 商品图片 数据层
 * 
 * @author campus
 */
public interface CtProductImageMapper
{
    public List<CtProductImage> selectCtProductImageList(CtProductImage ctProductImage);

    public CtProductImage selectCtProductImageById(Long imageId);

    public int insertCtProductImage(CtProductImage ctProductImage);

    public int updateCtProductImage(CtProductImage ctProductImage);

    public int deleteCtProductImageById(Long imageId);

    public int deleteCtProductImageByIds(Long[] imageIds);

    public int deleteCtProductImageByProductId(Long productId);
}
