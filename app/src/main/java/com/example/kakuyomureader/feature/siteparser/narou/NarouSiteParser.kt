package com.example.kakuyomureader.feature.siteparser.narou

import com.example.kakuyomureader.core.model.SiteType
import com.example.kakuyomureader.feature.siteparser.api.NovelSiteParser

/**
 * 小説家になろう専用パーサー実装 (Plugin)
 *
 * [責務]: 小説家になろう (syosetu.com / ncode.syosetu.com) の本文構造 (#novel_honbun p 等) に対応したJSスクリプトを提供する。
 * [影響する状態]: WebView内のDOM操作・ハイライト。
 * [発生しうる例外・エラー]: なし。
 */
class NarouSiteParser : NovelSiteParser {

    override val siteType: SiteType = SiteType.NAROU

    override fun canHandle(url: String): Boolean {
        return url.contains("syosetu.com", ignoreCase = true)
    }

    override fun getInjectionScript(): String {
        return """
            (function() {
                if (window.hasInjectedNarouReader) return;
                window.hasInjectedNarouReader = true;

                // スタイル注入
                let style = document.createElement('style');
                style.innerHTML = `
                    .novel-reader-highlight {
                        background-color: #E8F5E9 !important;
                        transition: background-color 0.3s ease;
                        border-left: 4px solid #4CAF50 !important;
                        padding-left: 4px !important;
                    }
                `;
                document.head.appendChild(style);

                // 段落に一意なIDを付与
                function assignParagraphIds() {
                    let paragraphs = document.querySelectorAll('#novel_honbun p, .novel_subtitle, .novel_view p');
                    if (paragraphs.length === 0) {
                        paragraphs = document.querySelectorAll('p');
                    }
                    paragraphs.forEach((p, idx) => {
                        if (!p.getAttribute('data-reader-id')) {
                            p.setAttribute('data-reader-id', 'narou_p_' + idx);
                        }
                    });
                }
                assignParagraphIds();

                // クリック監視
                document.addEventListener('click', function(e) {
                    let target = e.target;
                    if (target.closest('button, nav, header, footer')) return;

                    let p = target.closest('#novel_honbun p, [data-reader-id], p');
                    if (!p) return;

                    let paragraphId = p.getAttribute('data-reader-id') || 'narou_p_dyn';
                    let clone = p.cloneNode(true);
                    clone.querySelectorAll('rt, rp').forEach(rt => rt.remove());

                    let text = (clone.innerText || clone.textContent || '').trim();
                    if (text.length === 0) return;

                    window.AndroidBridge && window.AndroidBridge.onParagraphClicked(paragraphId, text);

                    // 親コンテナ（#novel_honbun 等）から全段落を一括抽出
                    let container = p.closest('#novel_honbun') || p.parentElement;
                    if (container) {
                        let allPs = Array.from(container.querySelectorAll('p'));
                        let targetIdx = allPs.indexOf(p);
                        let start = targetIdx >= 0 ? targetIdx : 0;
                        let extracted = [];
                        for (let i = start; i < allPs.length; i++) {
                            let itemEl = allPs[i];
                            let c = itemEl.cloneNode(true);
                            c.querySelectorAll('rt, rp').forEach(rt => rt.remove());
                            let t = (c.innerText || c.textContent || '').trim();
                            if (t.length > 0) {
                                let pid = itemEl.getAttribute('data-reader-id') || ('narou_p_' + i);
                                itemEl.setAttribute('data-reader-id', pid);
                                extracted.push({ id: pid, text: t });
                            }
                        }
                        if (extracted.length > 0) {
                            window.AndroidBridge && window.AndroidBridge.onContainerParagraphsExtracted(JSON.stringify(extracted));
                        }
                    }
                }, true);

                // 自動連続再生用の次段落探索
                window.NovelReaderGetNext = function(currentId) {
                    let paragraphs = Array.from(document.querySelectorAll('#novel_honbun p, .novel_subtitle, .novel_view p, p'));
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
                            let nextId = el.getAttribute('data-reader-id') || ('narou_p_' + i);
                            el.setAttribute('data-reader-id', nextId);
                            window.AndroidBridge && window.AndroidBridge.onNextParagraphFound(nextId, text);
                            return;
                        }
                    }

                    window.AndroidBridge && window.AndroidBridge.onEndOfEpisode();
                };

                // ページ内全段落の一括抽出関数 (BG再生用)
                window.NovelReaderExtractAll = function() {
                    let paragraphs = Array.from(document.querySelectorAll('#novel_honbun p, .novel_view p, p'));
                    let extracted = [];
                    for (let i = 0; i < paragraphs.length; i++) {
                        let itemEl = paragraphs[i];
                        if (itemEl.closest('button, nav, header, footer, #novel_header, #novel_footer')) continue;
                        let c = itemEl.cloneNode(true);
                        c.querySelectorAll('rt, rp').forEach(rt => rt.remove());
                        let t = (c.innerText || c.textContent || '').trim();
                        if (t.length > 0) {
                            let pid = itemEl.getAttribute('data-reader-id') || ('narou_p_' + i);
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
                let nextLink = document.querySelector('.novel_bn a:last-child, a.next');
                if (!nextLink) {
                    let links = Array.from(document.querySelectorAll('a'));
                    nextLink = links.find(a => {
                        let text = (a.textContent || '').trim();
                        return text.includes('次へ') || text.includes('次の話');
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
