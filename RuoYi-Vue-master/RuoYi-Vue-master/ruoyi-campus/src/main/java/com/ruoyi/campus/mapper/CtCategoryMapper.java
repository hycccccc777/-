package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtCategory;

/**
 * 商品类目 数据层
 * 
 * @author campus
 */
public interface CtCategoryMapper
{
    public List<CtCategory> selectCtCategoryList(CtCategory ctCategory);

    public CtCategory selectCtCategoryById(Long categoryId);

    public int insertCtCategory(CtCategory ctCategory);

    public int updateCtCategory(CtCategory ctCategory);

    public int deleteCtCategoryById(Long categoryId);

    public int deleteCtCategoryByIds(Long[] categoryIds);
}
