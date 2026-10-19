package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtCategory;
import com.ruoyi.campus.mapper.CtCategoryMapper;
import com.ruoyi.campus.service.ICtCategoryService;

/**
 * 商品类目 服务层实现
 * 
 * @author campus
 */
@Service
public class CtCategoryServiceImpl implements ICtCategoryService
{
    @Autowired
    private CtCategoryMapper ctCategoryMapper;

    @Override
    public List<CtCategory> selectCtCategoryList(CtCategory ctCategory)
    {
        return ctCategoryMapper.selectCtCategoryList(ctCategory);
    }

    @Override
    public CtCategory selectCtCategoryById(Long categoryId)
    {
        return ctCategoryMapper.selectCtCategoryById(categoryId);
    }

    @Override
    public int insertCtCategory(CtCategory ctCategory)
    {
        return ctCategoryMapper.insertCtCategory(ctCategory);
    }

    @Override
    public int updateCtCategory(CtCategory ctCategory)
    {
        return ctCategoryMapper.updateCtCategory(ctCategory);
    }

    @Override
    public int deleteCtCategoryByIds(Long[] categoryIds)
    {
        return ctCategoryMapper.deleteCtCategoryByIds(categoryIds);
    }
}
