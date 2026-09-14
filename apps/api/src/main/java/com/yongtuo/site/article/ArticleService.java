package com.yongtuo.site.article;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.content.ContentPage;
import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.content.PublicationPolicy;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import com.yongtuo.site.site.UrlRedirectService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleService {
    private final ArticleRepository repository;
    private final UrlRedirectService redirects;
    public ArticleService(ArticleRepository repository, UrlRedirectService redirects) {
        this.repository = repository; this.redirects = redirects;
    }
    @Transactional(readOnly=true)
    public ContentPage<?> list(boolean publicOnly, boolean en, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw invalid("Invalid pagination");
        var rows = repository.list(publicOnly, en, page, size);
        long total = repository.count(publicOnly, en);
        java.util.List<?> items = publicOnly ? rows.stream().map(a -> PublicArticle.from(a,en)).toList() : rows;
        return new ContentPage<>(items,page,size,total,(total+size-1)/size);
    }
    @Transactional(readOnly=true)
    public ContentPage<?> publicList(boolean en, int page, int size, String category) {
        if(category == null || category.isBlank()) return list(true, en, page, size);
        if(page < 1 || size < 1 || size > 100) throw invalid("Invalid pagination");
        var items = repository.publicCategoryList(en, page, size, category).stream().map(a -> PublicArticle.from(a,en)).toList();
        long total = repository.publicCategoryCount(en, category);
        return new ContentPage<>(items, page, size, total, (total+size-1)/size);
    }
    public Article get(long id) { return repository.find(id).orElseThrow(ArticleService::missing); }
    public PublicArticle publicDetail(String slug, boolean en) {
        return PublicArticle.from(repository.publicDetail(slug,en).orElseThrow(ArticleService::missing), en);
    }
    @Transactional
    public Article save(Long id, ArticleInput input) {
        repository.lockWrites();
        Article old = id == null ? null : get(id);
        if (!repository.categoryActive(input.categoryId())) throw invalid("Article category is invalid");
        if (input.isFeatured() && repository.featuredCount(id == null ? -1 : id) >= 3)
            throw invalid("At most three featured articles");
        if (input.status() == ProductStatus.PUBLISHED) {
            if (input.languageMode() != LanguageMode.EN_ONLY && (blank(input.titleZh()) || blank(clean(input.contentZh()))))
                throw invalid("Chinese title and content are required");
            if (input.languageMode() == LanguageMode.EN_ONLY && input.englishStatus() != EnglishStatus.CONFIRMED)
                throw invalid("English publication requires confirmation");
        }
        if (input.englishStatus() == EnglishStatus.CONFIRMED && (blank(input.titleEn()) || blank(clean(input.contentEn()))))
            throw invalid("Confirmed English title and content are required");
        try {
            long savedId = repository.save(id,input);
            boolean zh = PublicationPolicy.visible(input.languageMode(),input.englishStatus(),input.status(),false);
            boolean en = PublicationPolicy.visible(input.languageMode(),input.englishStatus(),input.status(),true);
            redirects.prepareContentCanonical("articles",input.slug(),zh,en);
            if (old != null) redirects.recordContentSlugChange("articles",old.slug(),input.slug(),
                    zh && PublicationPolicy.visible(old.languageMode(),old.englishStatus(),old.status(),false),
                    en && PublicationPolicy.visible(old.languageMode(),old.englishStatus(),old.status(),true));
            return get(savedId);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(30002,"Article slug already exists",HttpStatus.CONFLICT);
        }
    }
    @Transactional public void delete(long id) { repository.lockWrites(); get(id); repository.delete(id); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    static BusinessException invalid(String message) { return new BusinessException(30003,message,HttpStatus.BAD_REQUEST); }
    static BusinessException missing() { return new BusinessException(30001,"Article not found",HttpStatus.NOT_FOUND); }
}
