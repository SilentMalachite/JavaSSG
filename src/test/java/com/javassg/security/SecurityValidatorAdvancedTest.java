package com.javassg.security;

import com.javassg.model.SecurityLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityValidator Advanced Security Tests")
class SecurityValidatorAdvancedTest {

    private SecurityValidator validator;

    @BeforeEach
    void setUp() {
        SecurityLimits limits = SecurityLimits.defaultLimits();
        validator = new SecurityValidator(limits);
    }

    @Test
    @DisplayName("マルチライン攻撃を検出すること")
    void shouldDetectMultilineAttacks() {
        String multilineScript = """
            <script
            type="text/javascript">
            alert('xss');
            </script>
            """;

        assertThatThrownBy(() -> validator.validateContent(multilineScript))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");
    }

    @Test
    @DisplayName("すべての危険なイベントハンドラを検出すること")
    void shouldDetectAllDangerousEventHandlers() {
        String[] dangerousHandlers = {
            "<img onload='alert(1)'>",
            "<div onmouseover='steal()'>",
            "<input onkeydown='log()'>",
            "<body onunload='destroy()'>",
            "<form onsubmit='hijack()'>",
            "<video onplay='track()'>",
            "<audio onended='report()'>",
            "<canvas oncontextmenu='capture()'>",
            "<div ondragstart='steal()'>",
            "<input onpaste='intercept()'>"
        };

        for (String handler : dangerousHandlers) {
            assertThatThrownBy(() -> validator.validateContent(handler))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("危険なイベントハンドラが検出されました")
                .withFailMessage("Should detect: " + handler);
        }
    }

    @Test
    @DisplayName("スペースがあるイベントハンドラを検出すること")
    void shouldDetectEventHandlersWithSpaces() {
        String handlerWithSpaces1 = "<img onclick = 'alert(1)'>";
        String handlerWithSpaces2 = "<div onload   ='steal()'>";
        String handlerWithSpaces3 = "<input onchange\t='log()'>";

        assertThatThrownBy(() -> validator.validateContent(handlerWithSpaces1))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なイベントハンドラが検出されました");

        assertThatThrownBy(() -> validator.validateContent(handlerWithSpaces2))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なイベントハンドラが検出されました");

        assertThatThrownBy(() -> validator.validateContent(handlerWithSpaces3))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なイベントハンドラが検出されました");
    }

    @Test
    @DisplayName("自己終了タグを検出すること")
    void shouldDetectSelfClosingDangerousTags() {
        String selfClosingEmbed = "<embed src='malicious.swf' />";
        String selfClosingLink = "<link rel='stylesheet' href='malicious.css' />";
        String selfClosingMeta = "<meta http-equiv='refresh' content='0; url=evil.com' />";

        assertThatThrownBy(() -> validator.validateContent(selfClosingEmbed))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("embedタグが検出されました");

        // linkとmetaはサニタイゼーションでテストするが、validateContentでは検出しない
        // 実際の使用ではこれらはテンプレート外では通常使用されない
    }

    @Test
    @DisplayName("自己終了タグのサニタイゼーションが正しく動作すること")
    void shouldSanitizeSelfClosingTags() {
        String html = """
            <p>Safe content</p>
            <embed src='malicious.swf' />
            <link rel='stylesheet' href='malicious.css' />
            <meta http-equiv='refresh' content='0; url=evil.com' />
            <img src='safe.jpg' />
            """;

        String sanitized = validator.sanitizeHtml(html);

        assertThat(sanitized).contains("<p>Safe content</p>");
        assertThat(sanitized).contains("<img src='safe.jpg' />");
        assertThat(sanitized).doesNotContain("<embed");
        assertThat(sanitized).doesNotContain("<link");
        assertThat(sanitized).doesNotContain("<meta");
    }

    @Test
    @DisplayName("ネストしたマルチライン攻撃を検出すること")
    void shouldDetectNestedMultilineAttacks() {
        String nestedAttack = """
            <div>
                <script>
                    if (true) {
                        alert('nested attack');
                    }
                </script>
            </div>
            """;

        assertThatThrownBy(() -> validator.validateContent(nestedAttack))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");
    }

    @Test
    @DisplayName("大文字小文字を混在した攻撃を検出すること")
    void shouldDetectMixedCaseAttacks() {
        String mixedCaseScript = "<ScRiPt>alert('mixed case attack')</ScRiPt>";
        String mixedCaseEvent = "<DiV OnClIcK='steal()'>Click me</DiV>";
        String mixedCaseIframe = "<IFrAmE src='http://evil.com'></IFrAmE>";

        assertThatThrownBy(() -> validator.validateContent(mixedCaseScript))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");

        assertThatThrownBy(() -> validator.validateContent(mixedCaseEvent))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なイベントハンドラが検出されました");

        assertThatThrownBy(() -> validator.validateContent(mixedCaseIframe))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("iframeタグが検出されました");
    }

    @Test
    @DisplayName("複数の攻撃パターンが混在していても検出すること")
    void shouldDetectMultipleAttackPatterns() {
        String multipleAttacks = """
            <p>Normal content</p>
            <script>alert('script attack')</script>
            <img onload='alert("event attack")' src='x.jpg'>
            <iframe src='http://evil.com'></iframe>
            <object data='malicious.pdf'></object>
            """;

        // 最初に見つかった攻撃（script）でエラーになる
        assertThatThrownBy(() -> validator.validateContent(multipleAttacks))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");
    }

    @Test
    @DisplayName("安全なHTMLコンテンツは検証を通ること")
    void shouldPassSafeHtmlContent() {
        String safeHtml = """
            <h1>安全なタイトル</h1>
            <p>これは安全な段落です。</p>
            <ul>
                <li>リスト項目1</li>
                <li>リスト項目2</li>
            </ul>
            <blockquote>引用文です</blockquote>
            <code>var safe = 'code';</code>
            <pre>preformatted text</pre>
            <strong>強調</strong>
            <em>斜体</em>
            <br>
            <hr>
            <img src='safe-image.jpg' alt='説明文'>
            <a href='https://safe-site.com'>安全なリンク</a>
            """;

        assertThatCode(() -> validator.validateContent(safeHtml))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("エッジケースの攻撃パターンを検出すること")
    void shouldDetectEdgeCaseAttacks() {
        // スクリプトタグ内にコメントアウトされたコード
        String commentedScript = "<script><!-- alert('still dangerous'); --></script>";
        
        // 改行を含むイベントハンドラ
        String multilineEvent = """
            <div onclick="
                alert('multiline event');
                steal_data();
            ">Click me</div>
            """;

        assertThatThrownBy(() -> validator.validateContent(commentedScript))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なスクリプトタグが検出されました");

        assertThatThrownBy(() -> validator.validateContent(multilineEvent))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("危険なイベントハンドラが検出されました");
    }
}