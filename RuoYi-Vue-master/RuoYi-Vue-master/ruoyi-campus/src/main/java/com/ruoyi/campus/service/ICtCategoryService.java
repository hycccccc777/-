package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtCategory;

/**
 * 商品类目 服务层
 * 
 * @author campus
 */
public interface ICtCategoryService
{
    public List<CtCategory> selectCtCategoryList(CtCategory ctCategory);

    public CtCategory selectCtCategoryById(Long categoryId);

    public int insertCtCategory(CtCategory ctCategory);

    public int updateCtCategory(CtCategory ctCategory);

    public int deleteCtCategoryByIds(Long[] categoryIds);
}
