package com.yongtuo.site.article;

import com.yongtuo.site.common.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleCategoryService {
    private final JdbcTemplate jdbc;
    private static final DataClassRowMapper<ArticleCategory> ROW = new DataClassRowMapper<>(ArticleCategory.class);
    public ArticleCategoryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<ArticleCategory> list() { return jdbc.query("SELECT * FROM article_category ORDER BY sort_order,id", ROW); }
    public List<Map<String,Object>> publicList(boolean en) {
        return list().stream().filter(c -> c.status().equals("ACTIVE") && (!en || c.nameEn() != null && !c.nameEn().isBlank()))
                .map(c -> Map.<String,Object>of("id",c.id(),"name",en ? c.nameEn() : c.nameZh(),"slug",c.slug())).toList();
    }
    @Transactional
    public ArticleCategory save(Long id, ArticleCategoryInput input) {
        try {
            if (id == null) {
                jdbc.update("INSERT INTO article_category(name_zh,name_en,slug,sort_order,status) VALUES (?,?,?,?,?)",
                        input.nameZh(),input.nameEn(),input.slug(),input.sortOrder(),input.status());
                id = jdbc.queryForObject("SELECT id FROM article_category WHERE slug=?",Long.class,input.slug());
            } else if (jdbc.update("UPDATE article_category SET name_zh=?,name_en=?,slug=?,sort_order=?,status=? WHERE id=?",
                    input.nameZh(),input.nameEn(),input.slug(),input.sortOrder(),input.status(),id) == 0) {
                throw new BusinessException(30004,"Article category not found",HttpStatus.NOT_FOUND);
            }
            return jdbc.queryForObject("SELECT * FROM article_category WHERE id=?",ROW,id);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(30005,"Article category slug already exists",HttpStatus.CONFLICT);
        }
    }
    @Transactional
    public void delete(long id) {
        var ids = jdbc.queryForList("SELECT id FROM article_category WHERE id=? FOR UPDATE",Long.class,id);
        if (ids.isEmpty()) throw new BusinessException(30004,"Article category not found",HttpStatus.NOT_FOUND);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE category_id=?",Long.class,id)>0)
            throw ArticleService.invalid("Article category is in use; deactivate it instead");
        jdbc.update("DELETE FROM article_category WHERE id=?",id);
    }
}
