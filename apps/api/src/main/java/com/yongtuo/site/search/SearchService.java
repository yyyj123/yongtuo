package com.yongtuo.site.search;

import com.yongtuo.site.product.query.PublicProductDto;
import com.yongtuo.site.product.query.PublicProductDtoFactory;
import com.yongtuo.site.product.query.PublicProductPage;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {
    static final int RESULT_LIMIT = 24;
    static final int SUGGESTION_LIMIT = 10;

    private final SearchMapper mapper;

    public SearchService(SearchMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PublicProductPage search(String rawKeyword, Locale locale) {
        String keyword = normalize(rawKeyword);
        if (keyword == null) return new PublicProductPage(List.of(), 1, RESULT_LIMIT, 0, 0);
        boolean english = isEnglish(locale);
        long total = mapper.count(keyword, english);
        List<PublicProductDto> items = total == 0 ? List.of()
                : mapper.find(keyword, english, RESULT_LIMIT).stream()
                .map(product -> PublicProductDtoFactory.summary(product, english)).toList();
        int pages = total == 0 ? 0 : (int) ((total + RESULT_LIMIT - 1) / RESULT_LIMIT);
        return new PublicProductPage(items, 1, RESULT_LIMIT, total, pages);
    }

    @Transactional(readOnly = true)
    public List<SearchSuggestion> suggestions(String rawQuery, Locale locale) {
        String query = normalize(rawQuery);
        if (query == null) return List.of();
        boolean english = isEnglish(locale);
        return mapper.find(query, english, SUGGESTION_LIMIT).stream()
                .map(product -> new SearchSuggestion(english ? product.getNameEn() : product.getNameZh(),
                        product.getSlug(), product.getProductCode()))
                .toList();
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean isEnglish(Locale locale) {
        return locale != null && Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage());
    }
}
