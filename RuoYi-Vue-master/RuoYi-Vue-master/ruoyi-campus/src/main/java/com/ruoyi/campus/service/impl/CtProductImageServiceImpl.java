package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtProductImage;
import com.ruoyi.campus.mapper.CtProductImageMapper;
import com.ruoyi.campus.service.ICtProductImageService;

/**
 * 商品图片 服务层实现
 * 
 * @author campus
 */
@Service
public class CtProductImageServiceImpl implements ICtProductImageService
{
    @Autowired
    private CtProductImageMapper ctProductImageMapper;

    @Override
    public List<CtProductImage> selectCtProductImageList(CtProductImage ctProductImage)
    {
        return ctProductImageMapper.selectCtProductImageList(ctProductImage);
    }

    @Override
    public CtProductImage selectCtProductImageById(Long imageId)
    {
        return ctProductImageMapper.selectCtProductImageById(imageId);
    }

    @Override
    public int insertCtProductImage(CtProductImage ctProductImage)
    {
        return ctProductImageMapper.insertCtProductImage(ctProductImage);
    }

    @Override
    public int updateCtProductImage(CtProductImage ctProductImage)
    {
        return ctProductImageMapper.updateCtProductImage(ctProductImage);
    }

    @Override
    public int deleteCtProductImageByIds(Long[] imageIds)
    {
        return ctProductImageMapper.deleteCtProductImageByIds(imageIds);
    }

    @Override
    public int deleteCtProductImageByProductId(Long productId)
    {
        return ctProductImageMapper.deleteCtProductImageByProductId(productId);
    }
}
