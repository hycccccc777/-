package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtFavorite;

/**
 * 商品收藏 服务层
 * 
 * @author campus
 */
public interface ICtFavoriteService
{
    public List<CtFavorite> selectCtFavoriteList(CtFavorite ctFavorite);

    public CtFavorite selectCtFavoriteById(Long favoriteId);

    public boolean isFavorite(CtFavorite ctFavorite);

    public int insertCtFavorite(CtFavorite ctFavorite);

    public int deleteCtFavoriteByIds(Long[] favoriteIds);

    public int deleteCtFavoriteByUserAndProduct(CtFavorite ctFavorite);
}
