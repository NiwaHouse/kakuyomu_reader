package com.example.kakuyomureader.feature.siteparser.generic

import com.example.kakuyomureader.core.model.SiteType
import com.example.kakuyomureader.feature.siteparser.api.NovelSiteParser

/**
 * 汎用Webサイト用パーサー実装 (Plugin Fallback)
 *
 * [責務]: 特定サイト用パーサーに合致しない一般的なWebページ（<p>, <article>, <div>等）に対応したスクリプトを提供する。
 * [影響する状態]: WebView内のDOM操作・ハイライト。
 * [発生しうる例外・エラー]: なし。
 */
class GenericSiteParser : NovelSiteParser {

    override val siteType: SiteType = SiteType.GENERIC

    override fun canHandle(url: String): Boolean = true // すべてのURLに対応

    override fun getInjectionScript(): String {
        return """
            (function() {
                if (window.hasInjectedGenericReader) return;
                window.hasInjectedGenericReader = true;

                let style = document.createElement('style');
                style.innerHTML = `
                    .novel-reader-highlight {
                        background-color: #E1F5FE !important;
                        transition: background-color 0.3s ease;
                        border-left: 4px solid #03A9F4 !important;
                        padding-left: 4px !important;
                    }
                `;
                document.head.appendChild(style);

                function assignParagraphIds() {
                    let paragraphs = document.querySelectorAll('p, article p, section p, .content p');
                    paragraphs.forEach((p, idx) => {
                        if (!p.getAttribute('data-reader-id')) {
                            p.setAttribute('data-reader-id', 'gen_p_' + idx);
                        }
                    });
                }
                assignParagraphIds();

                document.addEventListener('click', function(e) {
                    let target = e.target;
                    if (target.closest('button, nav, header, footer')) return;

                    let p = target.closest('p, article, section, div, [data-reader-id]');
                    if (!p) return;

                    let paragraphId = p.getAttribute('data-reader-id') || 'gen_p_dyn';

                    let clone = p.cloneNode(true);
                    clone.querySelectorAll('rt, rp').forEach(rt => rt.remove());

                    let text = (clone.innerText || clone.textContent || '').trim();
                    if (text.length === 0) return;

                    window.AndroidBridge && window.AndroidBridge.onParagraphClicked(paragraphId, text);

                    // 親コンテナ（article, main, または2-3階層上の親要素）から段落を一括抽出
                    let container = p.closest('article, main, .content, .entry-content, .post-content') || p.parentElement?.parentElement || p.parentElement;
                    if (container) {
                        let allPs = Array.from(container.querySelectorAll('p, div, li, h1, h2, h3, h4, h5, h6'));
                        let targetIdx = allPs.indexOf(p);
                        let start = targetIdx >= 0 ? targetIdx : 0;
                        let extracted = [];
                        for (let i = start; i < allPs.length; i++) {
                            let itemEl = allPs[i];
                            // 子要素にpを持つ要素は重複防止でスキップ
                            if (itemEl.tagName !== 'P' && itemEl.querySelector('p')) continue;
                            let c = itemEl.cloneNode(true);
                            c.querySelectorAll('rt, rp').forEach(rt => rt.remove());
                            let t = (c.innerText || c.textContent || '').trim();
                            if (t.length > 0) {
                                let pid = itemEl.getAttribute('data-reader-id') || ('gen_p_' + i);
                                itemEl.setAttribute('data-reader-id', pid);
                                extracted.push({ id: pid, text: t });
                            }
                        }
                        if (extracted.length > 0) {
                            window.AndroidBridge && window.AndroidBridge.onContainerParagraphsExtracted(JSON.stringify(extracted));
                        }
                    }
                }, true);

                window.NovelReaderGetNext = function(currentId) {
                    let paragraphs = Array.from(document.querySelectorAll('p, article p, section p, .content p'));
                    if (paragraphs.length === 0) {
                        window.AndroidBridge && window.AndroidBridge.onEndOfEpisode();
                        return;
                    }

                    let currentIndex = paragraphs.findIndex(el => el.getAttribute('data-reader-id') === currentId);
                    let startIndex = currentIndex >= 0 ? currentIndex + 1 : 0;
                    for (let i = startIndex; i < paragraphs.length; i++) {
                        let el = paragraphs[i];
                        let clone = el.cloneNode(true);
                        clone.querySelectorAll('rt, rp').forEach(rt => rt.remove());
                        let text = (clone.innerText || clone.textContent || '').trim();
                        if (text.length > 0) {
                            let nextId = el.getAttribute('data-reader-id') || ('gen_p_' + i);
                            el.setAttribute('data-reader-id', nextId);
                            window.AndroidBridge && window.AndroidBridge.onNextParagraphFound(nextId, text);
                            return;
                        }
                    }

                    window.AndroidBridge && window.AndroidBridge.onEndOfEpisode();
                };

                // ページ内全段落の一括抽出関数 (BG再生用)
                window.NovelReaderExtractAll = function() {
                    let paragraphs = Array.from(document.querySelectorAll('article p, main p, .content p, section p, p'));
                    let extracted = [];
                    for (let i = 0; i < paragraphs.length; i++) {
                        let itemEl = paragraphs[i];
                        if (itemEl.closest('button, nav, header, footer, noscript, script, style')) continue;
                        let c = itemEl.cloneNode(true);
                        c.querySelectorAll('rt, rp').forEach(rt => rt.remove());
                        let t = (c.innerText || c.textContent || '').trim();
                        if (t.length > 0) {
                            let pid = itemEl.getAttribute('data-reader-id') || ('gen_p_' + i);
                            itemEl.setAttribute('data-reader-id', pid);
                            extracted.push({ id: pid, text: t });
                        }
                    }
                    if (extracted.length > 0) {
                        window.AndroidBridge && window.AndroidBridge.onContainerParagraphsExtracted(JSON.stringify(extracted));
                    }
                };
            })();
        """.trimIndent()
    }

    override fun getHighlightScript(paragraphId: String): String {
        return """
            (function() {
                document.querySelectorAll('.novel-reader-highlight').forEach(el => {
                    el.classList.remove('novel-reader-highlight');
                });
                let target = document.querySelector('[data-reader-id="$paragraphId"]');
                if (target) {
                    target.classList.add('novel-reader-highlight');
                    target.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            })();
        """.trimIndent()
    }

    override fun getNextParagraphScript(currentParagraphId: String): String {
        return "window.NovelReaderGetNext && window.NovelReaderGetNext('$currentParagraphId');"
    }

    override fun getClearHighlightScript(): String {
        return """
            document.querySelectorAll('.novel-reader-highlight').forEach(el => {
                el.classList.remove('novel-reader-highlight');
            });
        """.trimIndent()
    }

    override fun getNextEpisodeScript(): String {
        return """
            (function() {
                let nextLink = document.querySelector('a[rel="next"], .next a, a.next');
                if (!nextLink) {
                    let links = Array.from(document.querySelectorAll('a'));
                    nextLink = links.find(a => {
                        let text = (a.textContent || '').trim();
                        return text.includes('次へ') || text.includes('次のページ') || text.includes('次話');
                    });
                }
                if (nextLink && nextLink.href) {
                    window.location.href = nextLink.href;
                } else {
                    window.AndroidBridge && window.AndroidBridge.onNoNextEpisode();
                }
            })();
        """.trimIndent()
    }
}
