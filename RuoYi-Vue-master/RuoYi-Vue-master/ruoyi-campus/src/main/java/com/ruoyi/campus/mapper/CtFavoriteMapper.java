package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtFavorite;

/**
 * 商品收藏 数据层
 * 
 * @author campus
 */
public interface CtFavoriteMapper
{
    public List<CtFavorite> selectCtFavoriteList(CtFavorite ctFavorite);

    public CtFavorite selectCtFavoriteById(Long favoriteId);

    public CtFavorite selectCtFavoriteByUserAndProduct(CtFavorite ctFavorite);

    public int insertCtFavorite(CtFavorite ctFavorite);

    public int deleteCtFavoriteById(Long favoriteId);

    public int deleteCtFavoriteByIds(Long[] favoriteIds);

    public int deleteCtFavoriteByUserAndProduct(CtFavorite ctFavorite);
}
