package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReactPrototypeGenerator {

  /**
   * Vite + React 최소 템플릿 파일 (경로 → UTF-8 내용).
   */
  public Map<String, String> generateFiles(Idea idea) {
    Map<String, String> files = new LinkedHashMap<>();
    String safeTitle = escapeJsString(summarizeTitle(idea.getContent()));
    String body = escapeForJsOneLine(escapeHtml(idea.getContent()));

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
            .formatted(safeTitle));

    files.put(
        "src/main.jsx",
        """
        import React from 'react'
        import { createRoot } from 'react-dom/client'
        import App from './App.jsx'
        createRoot(document.getElementById('root')).render(<App />)
        """);

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
                다음 단계로
              </button>
            </main>
          )
        }
        """
            .formatted(safeTitle, body));

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
              body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; margin: 0; background: #f8fafc; color: #0f172a; }
              .wrap { max-width: 760px; margin: 42px auto; padding: 0 20px; }
              .card { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 24px; box-shadow: 0 1px 2px rgba(0,0,0,.04); }
              h1 { margin: 0 0 12px; font-size: 28px; line-height: 1.25; }
              p { margin: 0; color: #334155; line-height: 1.75; white-space: pre-wrap; }
              button { margin-top: 24px; padding: 11px 18px; border-radius: 8px; border: 0; background: #01cd15; color: #fff; font-weight: 700; }
              .hint { margin-top: 14px; font-size: 12px; color: #64748b; }
            </style>
          </head>
          <body>
            <main class="wrap">
              <section class="card">
                <h1>%s</h1>
                <p>%s</p>
                <button type="button">다음 단계로</button>
                <div class="hint">AI generated prototype preview (server-hosted)</div>
              </section>
            </main>
          </body>
        </html>
        """
            .formatted(safeTitle, safeTitle, body.replace("\\n", "\n")));

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
    return oneLine.length() > 40 ? oneLine.substring(0, 40) + "…" : oneLine;
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

  /** JSX 문자열 리터럴 안에 넣을 수 있도록 줄바꿈을 이스케이프합니다. */
  private static String escapeForJsOneLine(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n");
  }
}
