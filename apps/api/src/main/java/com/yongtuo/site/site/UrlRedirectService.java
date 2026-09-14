package com.yongtuo.site.site;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlRedirectService {
    private static final int MAX_OLD_PATH_LENGTH = 512;

    private final JdbcTemplate jdbc;

    public UrlRedirectService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void prepareContentCanonical(String kind, String slug, boolean zh, boolean en) {
        requireContentKind(kind);
        if (zh) removeStaleCanonical("/" + kind + "/" + slug);
        if (en) removeStaleCanonical("/en/" + kind + "/" + slug);
    }

    @Transactional
    public void recordContentSlugChange(String kind, String oldSlug, String newSlug, boolean zh, boolean en) {
        requireContentKind(kind);
        if (oldSlug.equals(newSlug)) return;
        if (zh) record("/" + kind + "/" + oldSlug, "/" + kind + "/" + newSlug);
        if (en) record("/en/" + kind + "/" + oldSlug, "/en/" + kind + "/" + newSlug);
    }

    private static void requireContentKind(String kind) {
        if (!java.util.Set.of("articles", "cases").contains(kind)) throw new IllegalArgumentException("Invalid content kind");
    }

    @Transactional
    public void prepareProductCanonical(String slug, boolean chinesePublic, boolean englishPublic) {
        if (chinesePublic) removeStaleCanonical("/products/" + slug);
        if (englishPublic) removeStaleCanonical("/en/products/" + slug);
    }

    @Transactional
    public void prepareCategoryCanonical(String slug, boolean publicCategory) {
        if (!publicCategory) return;
        removeStaleCanonical("/products?category=" + slug);
        removeStaleCanonical("/en/products?category=" + slug);
    }

    @Transactional
    public void recordProductSlugChange(String oldSlug, String newSlug,
                                        boolean chinesePublic, boolean englishPublic) {
        if (oldSlug.equals(newSlug)) return;
        if (chinesePublic) record("/products/" + oldSlug, "/products/" + newSlug);
        if (englishPublic) record("/en/products/" + oldSlug, "/en/products/" + newSlug);
    }

    @Transactional
    public void recordCategorySlugChange(String oldSlug, String newSlug, boolean publicCategory) {
        if (!publicCategory || oldSlug.equals(newSlug)) return;
        record("/products?category=" + oldSlug, "/products?category=" + newSlug);
        record("/en/products?category=" + oldSlug, "/en/products?category=" + newSlug);
    }

    @Transactional(readOnly = true)
    public Optional<UrlRedirect> resolve(String oldPath) {
        if (oldPath == null || oldPath.isBlank() || oldPath.length() > MAX_OLD_PATH_LENGTH
                || oldPath.charAt(0) != '/') {
            return Optional.empty();
        }
        return jdbc.query("""
                        SELECT old_path, new_path, redirect_type
                        FROM url_redirect
                        WHERE old_path = ?
                        """,
                (result, rowNumber) -> new UrlRedirect(result.getString("old_path"),
                        result.getString("new_path"), result.getInt("redirect_type")), oldPath)
                .stream().findFirst();
    }

    private void record(String oldPath, String newPath) {
        removeStaleCanonical(newPath);
        jdbc.update("""
                UPDATE url_redirect
                SET new_path = ?, redirect_type = 301
                WHERE new_path = ? AND old_path <> ?
                """, newPath, oldPath, newPath);
        jdbc.update("""
                INSERT INTO url_redirect(old_path, new_path, redirect_type)
                VALUES (?, ?, 301)
                ON DUPLICATE KEY UPDATE new_path = VALUES(new_path), redirect_type = 301,
                                        created_at = CURRENT_TIMESTAMP
                """, oldPath, newPath);
    }

    private void removeStaleCanonical(String canonicalPath) {
        jdbc.update("DELETE FROM url_redirect WHERE old_path = ?", canonicalPath);
    }
}
