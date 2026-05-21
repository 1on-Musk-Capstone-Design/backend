package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Vite + React 최소 템플릿. {@code uiStructureJson}이 있으면 PRD/IA에 맞춘 **랜딩형** 화면을, 없으면
 * 기존 단일 카드 뷰를 생성합니다.
 */
@Slf4j
@Component
public class ReactPrototypeGenerator {

  private static final String APP_WITH_DATA =
      """
      import React from 'react'
      import data from './landingData.json'

      export default function App() {
        const route = data.routes && data.routes[0] ? data.routes[0] : { sections: [] }
        return (
          <main style={{ fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif', maxWidth: 900, margin: '0 auto', padding: '48px 20px 64px', color: '#0f172a' }}>
            {route.sections && route.sections.map((section, i) => {
              if (section.type === 'hero') {
                return (
                  <header key={i} style={{ marginBottom: 48, borderBottom: '1px solid #e2e8f0', paddingBottom: 32 }}>
                    <h1 style={{ fontSize: 32, fontWeight: 800, margin: '0 0 16px', lineHeight: 1.2 }}>{section.title || ''}</h1>
                    {section.subtitle ? <p style={{ fontSize: 18, color: '#475569', lineHeight: 1.7, margin: 0 }}>{section.subtitle}</p> : null}
                    {(section.bullets && section.bullets.length) ? (
                      <ul style={{ marginTop: 20, paddingLeft: 20, color: '#334155' }}>
                        {section.bullets.map((b, j) => <li key={j} style={{ marginBottom: 8 }}>{b}</li>)}
                      </ul>
                    ) : null}
                  </header>
                )
              }
              if (section.type === 'features') {
                return (
                  <section key={i} style={{ marginBottom: 40 }}>
                    <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 16 }}>{section.title || '핵심 가치'}</h2>
                    <ul style={{ lineHeight: 1.8, color: '#334155', paddingLeft: 20 }}>
                      {(section.bullets || []).map((b, j) => <li key={j} style={{ marginBottom: 6 }}>{b}</li>)}
                    </ul>
                  </section>
                )
              }
              if (section.type === 'cta') {
                const btn = (section.bullets && section.bullets[0]) || '시작하기'
                const sub = (section.bullets && section.bullets[1]) || ''
                return (
                  <section key={i} style={{ textAlign: 'center', marginTop: 48, padding: 40, background: '#f0fdf4', borderRadius: 16, border: '1px solid #bbf7d0' }}>
                    <h2 style={{ fontSize: 20, marginBottom: 12, fontWeight: 700 }}>{section.title || '다음 단계'}</h2>
                    {sub ? <p style={{ color: '#64748b', marginBottom: 20, fontSize: 15 }}>{sub}</p> : null}
                    <button type="button" style={{ padding: '14px 28px', borderRadius: 10, border: 'none', background: '#01cd15', color: '#fff', fontWeight: 700, fontSize: 16, cursor: 'pointer' }}>{btn}</button>
                  </section>
                )
              }
              return null
            })}
          </main>
        )
      }
      """;

  private final ObjectMapper objectMapper;

  public ReactPrototypeGenerator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, String> generateFiles(Idea idea) {
    return generateFiles(idea, null);
  }

  public Map<String, String> generateFiles(Idea idea, String uiStructureJson) {
    if (uiStructureJson != null && !uiStructureJson.isBlank()) {
      try {
        JsonNode root = objectMapper.readTree(uiStructureJson);
        if (root != null && root.isObject() && root.path("routes").isArray() && !root.path("routes").isEmpty()) {
          return buildFromUiStructure(idea, root, uiStructureJson);
        }
      } catch (Exception e) {
        log.warn("UI JSON 파싱 실패, 기본 뷰 사용: {}", e.getMessage());
      }
    }
    return buildSimpleSingleCard(idea);
  }

  private Map<String, String> buildFromUiStructure(Idea idea, JsonNode root, String rawJson) {
    Map<String, String> files = new LinkedHashMap<>();
    String appName = root.path("appName").asText(null);
    String pageTitle = (appName != null && !appName.isBlank()) ? appName : summarizeTitle(idea.getContent());
    addCommonViteFiles(files, pageTitle);

    files.put("src/landingData.json", rawJson);
    files.put("src/App.jsx", APP_WITH_DATA);

    files.put("preview/index.html", buildStaticPreviewFromUi(root, pageTitle, idea));
    files.put(".gitignore", "node_modules\ndist\n.DS_Store\n");
    return files;
  }

  private void addCommonViteFiles(Map<String, String> files, String pageTitle) {
    String titleEsc = escapeHtml(pageTitle);
    files.put(
        "package.json",
        """
        {
          "name": "idea-prototype",
          "private": true,
          "version": "0.0.1",
          "type": "module",
          "scripts": {
            "dev": "vite",
            "build": "vite build",
            "preview": "vite preview"
          },
          "dependencies": {
            "react": "^18.2.0",
            "react-dom": "^18.2.0"
          },
          "devDependencies": {
            "@vitejs/plugin-react": "^4.2.0",
            "vite": "^5.0.0"
          }
        }
        """);

    files.put(
        "vite.config.js",
        """
        import { defineConfig } from 'vite'
        import react from '@vitejs/plugin-react'
        export default defineConfig({ plugins: [react()] })
        """);

    files.put(
        "index.html",
        """
        <!doctype html>
        <html lang="ko">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%s</title>
          </head>
          <body>
            <div id="root"></div>
            <script type="module" src="/src/main.jsx"></script>
          </body>
        </html>
        """
            .formatted(titleEsc));

    files.put(
        "src/main.jsx",
        """
        import React from 'react'
        import { createRoot } from 'react-dom/client'
        import App from './App.jsx'
        createRoot(document.getElementById('root')).render(<App />)
        """);
  }

  private String buildStaticPreviewFromUi(JsonNode root, String pageTitle, Idea idea) {
    StringBuilder sb = new StringBuilder(2048);
    sb.append(
        """
        <!doctype html>
        <html lang="ko">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>""");
    sb.append(escapeHtml(pageTitle));
    sb.append(
        """
        </title>
            <style>
              body { font-family: system-ui, -apple-system, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; }
              .wrap { max-width: 860px; margin: 0 auto; padding: 40px 20px 56px; }
              header { border-bottom: 1px solid #e2e8f0; padding-bottom: 28px; margin-bottom: 32px; }
              h1 { font-size: 28px; margin: 0 0 12px; line-height: 1.2; }
              .lead { font-size: 17px; color: #475569; line-height: 1.7; margin: 0; }
              section { margin-bottom: 32px; }
              h2 { font-size: 20px; margin: 0 0 12px; }
              ul { margin: 0; padding-left: 20px; line-height: 1.75; color: #334155; }
              .cta { text-align: center; padding: 32px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 14px; }
              .btn { margin-top: 8px; padding: 12px 24px; border-radius: 10px; border: 0; background: #01cd15; color: #fff; font-weight: 700; font-size: 15px; }
              .hint { margin-top: 28px; font-size: 12px; color: #94a3b8; text-align: center; }
            </style>
          </head>
          <body>
            <div class="wrap">""");
    JsonNode sections = root.path("routes").path(0).path("sections");
    if (sections.isArray()) {
      for (JsonNode sec : sections) {
        String type = sec.path("type").asText("");
        if ("hero".equals(type)) {
          sb.append("<header><h1>")
              .append(escapeHtml(sec.path("title").asText("")))
              .append("</h1>");
          String sub = sec.path("subtitle").asText("");
          if (!sub.isEmpty()) {
            sb.append("<p class=\"lead\">").append(escapeHtml(sub)).append("</p>");
          }
          JsonNode bullets = sec.path("bullets");
          if (bullets.isArray() && bullets.size() > 0) {
            sb.append("<ul style=\"margin-top:16px\">");
            for (JsonNode b : bullets) {
              sb.append("<li>").append(escapeHtml(b.asText())).append("</li>");
            }
            sb.append("</ul>");
          }
          sb.append("</header>");
        } else if ("features".equals(type)) {
          sb.append("<section><h2>")
              .append(escapeHtml(sec.path("title").asText("핵심 가치")))
              .append("</h2><ul>");
          for (JsonNode b : sec.path("bullets")) {
            sb.append("<li>").append(escapeHtml(b.asText())).append("</li>");
          }
          sb.append("</ul></section>");
        } else if ("cta".equals(type)) {
          String b0 = sec.path("bullets").path(0).asText("시작하기");
          String b1 = sec.path("bullets").path(1).asText("");
          sb.append("<section class=\"cta\"><h2 style=\"margin-top:0\">")
              .append(escapeHtml(sec.path("title").asText("다음 단계")))
              .append("</h2>");
          if (!b1.isEmpty()) {
            sb.append("<p style=\"color:#64748b;margin-bottom:16px\">").append(escapeHtml(b1)).append("</p>");
          }
          sb.append("<button type=\"button\" class=\"btn\">")
              .append(escapeHtml(b0))
              .append("</button></section>");
        }
      }
    }
    sb.append("<p class=\"hint\">팀·이해관계자 공유를 위한 화면 미리보기입니다.</p></div></body></html>");
    return sb.toString();
  }

  private Map<String, String> buildSimpleSingleCard(Idea idea) {
    Map<String, String> files = new LinkedHashMap<>();
    String safeTitle = escapeJsString(summarizeTitle(idea.getContent()));
    String body = escapeForJsOneLine(escapeHtml(idea.getContent()));
    String pageTitle = summarizeTitle(idea.getContent());
    addCommonViteFiles(files, pageTitle);
    files.put(
        "src/App.jsx",
        """
        import React from 'react'
        export default function App() {
          return (
            <main style={{ fontFamily: 'system-ui', maxWidth: 720, margin: '48px auto', padding: '0 16px' }}>
              <h1 style={{ fontSize: 28, marginBottom: 12 }}>%s</h1>
              <p style={{ color: '#475569', lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>%s</p>
              <button type="button" style={{ marginTop: 24, padding: '12px 20px', borderRadius: 8, border: 'none', background: '#01cd15', color: '#fff', fontWeight: 700, cursor: 'pointer' }}>
                다음 단계
              </button>
            </main>
          )
        }
        """
            .formatted(safeTitle, body));
    String rawBody = idea.getContent() == null ? "" : idea.getContent();
    files.put(
        "preview/index.html",
        """
        <!doctype html>
        <html lang="ko">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%s</title>
            <style>
              body { font-family: system-ui, -apple-system, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; }
              .wrap { max-width: 760px; margin: 42px auto; padding: 0 20px; }
              .card { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 24px; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
              h1 { margin: 0 0 12px; font-size: 28px; line-height: 1.25; }
              p { margin: 0; color: #334155; line-height: 1.75; white-space: pre-wrap; }
              button { margin-top: 24px; padding: 11px 18px; border-radius: 8px; border: 0; background: #01cd15; color: #fff; font-weight: 700; }
              .hint { margin-top: 16px; font-size: 12px; color: #94a3b8; }
            </style>
          </head>
          <body>
            <main class="wrap">
              <section class="card">
                <h1>%s</h1>
                <p>%s</p>
                <button type="button">다음 단계</button>
                <p class="hint">팀·이해관계자 공유를 위한 화면 미리보기입니다. PRD·UI JSON이 있으면 섹션이 풍부해집니다.</p>
              </section>
            </main>
          </body>
        </html>
        """
            .formatted(
                escapeHtml(pageTitle),
                escapeHtml(summarizeTitle(idea.getContent())),
                escapeHtml(rawBody)));
    files.put(".gitignore", "node_modules\ndist\n.DS_Store\n");
    return files;
  }

  public byte[] toZipBytes(Map<String, String> files) {
    try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(bos)) {
      for (Map.Entry<String, String> e : files.entrySet()) {
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(e.getKey());
        zos.putNextEntry(entry);
        zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
      }
      zos.finish();
      return bos.toByteArray();
    } catch (java.io.IOException ex) {
      return new byte[0];
    }
  }

  private static String summarizeTitle(String content) {
    if (content == null || content.isBlank()) {
      return "Idea Prototype";
    }
    String oneLine = content.replace('\n', ' ').trim();
    if (oneLine.length() > 40) {
      return oneLine.substring(0, 40) + "…";
    }
    return oneLine;
  }

  private static String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static String escapeJsString(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
  }

  private static String escapeForJsOneLine(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n");
  }
}
