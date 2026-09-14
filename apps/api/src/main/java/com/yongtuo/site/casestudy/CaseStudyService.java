package com.yongtuo.site.casestudy;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.content.*;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import com.yongtuo.site.site.UrlRedirectService;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseStudyService {
    private final CaseStudyRepository repository;
    private final UrlRedirectService redirects;
    public CaseStudyService(CaseStudyRepository repository,UrlRedirectService redirects) { this.repository=repository;this.redirects=redirects; }
    @Transactional(readOnly=true)
    public ContentPage<?> list(boolean pub,boolean en,int page,int size) {
        if(page<1||size<1||size>100) throw invalid("Invalid pagination");
        var rows=repository.list(pub,en,page,size);
        List<?> items=pub?rows.stream().map(a->repository.publicView(a,en)).toList():rows.stream().map(repository::admin).toList();
        long total=repository.count(pub,en); return new ContentPage<>(items,page,size,total,(total+size-1)/size);
    }
    public CaseAdminDetail get(long id) { return repository.admin(require(id)); }
    public PublicCaseStudy publicDetail(String slug,boolean en) { return repository.publicView(repository.publicDetail(slug,en).orElseThrow(CaseStudyService::missing),en); }
    public List<CaseSummary> related(long productId,long categoryId,boolean en) { return repository.related(productId,categoryId,en); }
    @Transactional public CaseAdminDetail save(Long id,CaseStudyInput a) {
        repository.lockWrites();
        CaseStudy old=id==null?null:require(id);
        var products=ids(a.productIds()); var categories=ids(a.categoryIds());
        if(!repository.validReferences(products,categories)) throw invalid("Invalid case relation");
        if(a.isFeatured()&&repository.featuredCount(id==null?-1:id)>=4) throw invalid("At most four featured cases");
        if(a.status()==ProductStatus.PUBLISHED) {
            if(a.languageMode()!=LanguageMode.EN_ONLY&&(blank(a.titleZh())||blank(clean(a.contentZh())))) throw invalid("Chinese title and content are required");
            if(a.languageMode()==LanguageMode.EN_ONLY&&a.englishStatus()!=EnglishStatus.CONFIRMED) throw invalid("English publication requires confirmation");
        }
        if(a.englishStatus()==EnglishStatus.CONFIRMED&&(blank(a.titleEn())||blank(clean(a.contentEn())))) throw invalid("Confirmed English title and content are required");
        try {
            long saved=repository.save(id,a);
            repository.relations(saved,products,categories,a.imageUrls()==null?List.of():a.imageUrls());
            boolean zh=PublicationPolicy.visible(a.languageMode(),a.englishStatus(),a.status(),false);
            boolean en=PublicationPolicy.visible(a.languageMode(),a.englishStatus(),a.status(),true);
            redirects.prepareContentCanonical("cases",a.slug(),zh,en);
            if(old!=null) redirects.recordContentSlugChange("cases",old.slug(),a.slug(),
                    zh&&PublicationPolicy.visible(old.languageMode(),old.englishStatus(),old.status(),false),
                    en&&PublicationPolicy.visible(old.languageMode(),old.englishStatus(),old.status(),true));
            return get(saved);
        }catch(DuplicateKeyException exception) { throw new BusinessException(31002,"Case slug already exists",HttpStatus.CONFLICT); }
    }
    @Transactional public void delete(long id) { repository.lockWrites();require(id);repository.delete(id); }
    private CaseStudy require(long id) { return repository.find(id).orElseThrow(CaseStudyService::missing); }
    private static List<Long> ids(List<Long> values) { return values==null?List.of():values.stream().distinct().sorted().toList(); }
    private static boolean blank(String value) { return value==null||value.isBlank(); }
    private static BusinessException invalid(String message) { return new BusinessException(31003,message,HttpStatus.BAD_REQUEST); }
    private static BusinessException missing() { return new BusinessException(31001,"Case not found",HttpStatus.NOT_FOUND); }
}
