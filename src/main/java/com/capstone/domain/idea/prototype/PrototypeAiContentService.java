package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI가 설정된 경우 PRD·UI 구조를 LLM으로 생성하고, 실패 시 템플릿으로 폴백합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrototypeAiContentService {

  private final OpenAiHolder openAiHolder;
  private final PrdPrototypeGenerator templatePrd;
  private final UiStructureGenerator templateUi;
  private final ObjectMapper objectMapper;

  @Value("${openai.prototype-model:gpt-4o-mini}")
  private String prototypeModel;

  /** PRD는 긴 문서 + 보강 pass 대비 (모델 상한 내) */
  @Value("${openai.prototype-prd-max-tokens:8192}")
  private int prdMaxTokens;

  @Value("${openai.prototype-prd-min-chars:4500}")
  private int prdMinCharsForExpand;

  @Value("${openai.prototype-prd-brief-enabled:true}")
  private boolean prdBriefEnabled;

  @Value("${openai.prototype-prd-brief-min-content-chars:180}")
  private int prdBriefMinContentChars;

  @Value("${openai.prototype-prd-editor-pass-enabled:true}")
  private boolean prdEditorPassEnabled;

  public String generatePrd(Idea idea) {
    if (!openAiHolder.isEnabled()) {
      log.warn(
          "PRD: OpenAI 비활성(api-key 없음/형식 불일치) — 템플릿만 사용합니다. "
              + "src/main/resources/application-local.yml 또는 Capstone 루트 local-application.yml, "
              + "환경변수 OPENAI_API_KEY 를 확인하세요.");
      return templatePrd.generate(idea);
    }
    try {
      String briefJson = tryExtractStructuredBrief(idea);
      String raw =
          chatPrd(
              List.of(
                  Map.of("role", "system", "content", prdSystemPrompt()),
                  Map.of(
                      "role",
                      "user",
                      "content", buildPrdUserPrompt(idea, briefJson))));
      if (raw != null && !raw.isBlank()) {
        String out = normalizePrdMarkdown(raw.trim());
        out = expandPrdIfThin(out);
        out = prdEditorPassIfEnabled(out);
        log.info("PRD: OpenAI 응답 사용 (모델={}, {}자)", prototypeModel, out.length());
        return out;
      }
      log.warn("PRD: OpenAI 응답이 비어 있어 템플릿 사용");
      return templatePrd.generate(idea);
    } catch (Exception e) {
      log.warn(
          "LLM PRD 생성 실패, 템플릿 사용: {} ({})",
          e.getMessage(),
          e.getClass().getSimpleName());
      if (e.getMessage() != null
          && (e.getMessage().contains("401")
              || e.getMessage().toLowerCase().contains("invalid_api_key")
              || e.getMessage().toLowerCase().contains("incorrect api key"))) {
        log.warn("OpenAI 인증 실패 가능성: OPENAI_API_KEY 또는 local-application.yml 의 openai.api-key 를 확인하세요.");
      }
      return templatePrd.generate(idea);
    }
  }

  public String generateUiJson(Idea idea, String prdMarkdown) {
    if (!openAiHolder.isEnabled()) {
      log.warn("UI JSON: OpenAI 비활성 — 템플릿만 사용합니다.");
      return templateUi.generate(idea);
    }
    try {
      String raw =
          chatUi(
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      """
                      당신은 UX·랜딩 IA 설계자입니다. **단일 URL(/)** 랜딩 프로토타입에 쓰일 UI 정보구조를 **JSON만** 출력하세요.
                      PRD에 나온 제품명·가치·기능·CTA를 반영해 실제 앱에 가깝게 채웁니다(아이디어 원문만 덤프하지 마세요).

                      스키마(필드 준수):
                      {"version":1,"appName":string,
                       "routes":[{"path":"/","title":string,
                        "sections":[
                          {"type":"hero","title":string,"subtitle":string,"bullets":string[]},
                          {"type":"features","title":string,"bullets":string[]},
                          {"type":"cta","title":string,"bullets":string[]}
                        ]}],
                       "ideaSnippet":string}
                      - hero.subtitle: 한두 문장 가치 제안(비워 두지 말 것).
                      - features.bullets: 4~8개, PRD의 핵심 요구·기능에서 가져옴.
                      - cta.bullets[0]: 버튼 라벨, [1]이 있으면 보조 문구.
                      - JSON 이외 텍스트·코멘트 금지."""),
                  Map.of(
                      "role",
                      "user",
                      "content",
                      "아이디어 원문:\n"
                          + sanitizeIdeaContentForPrd(nullToEmpty(idea.getContent()))
                          + "\n\nPRD(우선):\n"
                          + nullToEmpty(prdMarkdown))));
      String cleaned = stripJsonFence(raw);
      JsonNode node = objectMapper.readTree(cleaned);
      if (node != null && node.isObject()) {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        log.info("UI JSON: OpenAI 응답 사용 (모델={}, {}자)", prototypeModel, json.length());
        return json;
      }
      log.warn("UI JSON: OpenAI 응답이 유효한 JSON 객체가 아니어 템플릿 사용");
    } catch (Exception e) {
      log.warn(
          "LLM UI JSON 생성 실패, 템플릿 사용: {} ({})",
          e.getMessage(),
          e.getClass().getSimpleName());
    }
    return templateUi.generate(idea);
  }

  private String chatPrd(List<Map<String, String>> messages) {
    return openAiHolder
        .getOpenAiChatService()
        .createChatCompletion(prototypeModel, messages, 0.32, prdMaxTokens);
  }

  private String chatUi(List<Map<String, String>> messages) {
    return openAiHolder
        .getOpenAiChatService()
        .createChatCompletion(prototypeModel, messages, 0.35, 4096);
  }

  private String chatExpand(List<Map<String, String>> messages) {
    return openAiHolder
        .getOpenAiChatService()
        .createChatCompletion(prototypeModel, messages, 0.28, prdMaxTokens);
  }

  private String prdSystemPrompt() {
    return """
        당신은 Staff 수준 **제품·B2B SaaS PM**입니다. **실제 스프린트 계획·개발 착수·검수**에 쓰일 **한국어 PRD**를 마크다운으로 **한 덩어리**만 출력하세요.

        ## 분량·깊이(필수)
        - 1~2줄 요약 수준은 **불합격**입니다. 각 `##`는 문단(3~8문장) **또는** 충분한 표/목록으로 채웁니다.
        - **전체 4,000~9,000자**를 목표(입력이 매우 짧으면 (가정)을 명시해 **합리적으로** 확장).
        - 추상어("편의성", "최적화")만 쓰지 말고, **행동·데이터·화면** 단위로 구체화.

        ## 반드시 포함할 뼈대(제목·순서 `##` 유지, 필요 시 `###`):
        1) `# (제품·서비스명)` — 첫 줄
        2) `## 문서 메타` — 표: | 항목 | 내용 | … 버전(0.1), 문서(PRD), 상태(초안), **주담당(TBD)**, **이해관계자(TBD)**
        3) `## 한 줄 요약(Executive summary)` — 비기술자용 **8~12문장**
        4) `## 배경·기회` — 시장/사용자, **왜 지금**인가
        5) `## 문제 정의` — pain, AS-IS 대안(스프레드시트·경쟁·수작업), 기회
        6) `## 제품 비전·목표` — **정성 목표 + 측정 가능한 KPI(숫자 또는 측정 정의)**
        7) `## 대상 사용자·페르소나` — **2~3명** | 이름·역할·목표·고충·채널(불릿+짧은 문단)
        8) `## 핵심 사용 시나리오` — **3~5개** | 트리거→단계→인풋→결과→성공 기준(번호 목록)
        9) `## 사용자 스토리` — **마크다운 표 필수**:
           | ID | As a(역할) | I want(행동) | So that(가치) | 수용 기준(검증 가능, Given/When/Then 가능) |
           P0 **최소 5행**, P1~P2 추가 가능
        10) `## 기능 요구사항` — 표:
            | ID | P0~P2 | 요구사항(무엇을) | 가치(왜) | 수용 기준(어떻게 검증) |
            **최소 8행**
        11) `## 엣지 케이스·오류·예외` — **6개 이상** 불릿(입력 누락, 권한, 네트워크, 데이터 충돌, 빈 상태 UX 등)
        12) `## 비기능 요구사항` — 보안·권한·PII, 성능(SLO/목표 수치 TBD라도 측정 항목 명시), 접근성, 감사 로그, 가용성
        13) `## 범위 밖(Non-goals)` — 이번에 **명시적으로 안** 하는 것
        14) `## 가정·제약` — (가정) / 법·일정·기술 제약
        15) `## 의존성·외부 시스템` — 인증, 결제, 푸시, **API**, 타 팀, 데이터 소스. 없으면 "없음" 또는 TBD+영향
        16) `## 데이터·엔터티(개요)` — 핵심 엔터티, 관계, 보존/삭제(PII) 한 줄씩
        17) `## MVP 정의 완료(DoD)` — **체크리스트 8항목 이상** (로그인·권한·핵심 플로우·에러·모니터링·문서·릴리스 기준 등)
        18) `## 용어집` — 약어·핵심 용어 **5개 이상** | 용어 | 정의
        19) `## 데이터·연동(상세·API)` — (15와 중복 최소화) 주요 API/이벤트/웹훅 TBD
        20) `## 리스크·완화` — **최소 4쌍** (리스크 / 완화)
        21) `## 롤아웃·릴리스` — MVP 범위, 단계, 롤백, 플래그/점진
        22) `## 열린 질문` — **5~8개** (결정 필요·우선순위 영향)

        ## 톤·금지
        - 제품·사용자·운영 중심. **검증 가능**한 수용 기준.
        - 금지: "파이프라인", "LLM", "OpenAI", "토큰", "자동 생성", "Vercel", 내부 툴 메타, "프로토타입 생성기" 등.
        - 응답은 PRD 본문만. 머리말·``` 코드펜스로 전체 감싸기 금지.
        - 입력이 카드·키워드 조각이면 **하나의 일관된 제품**으로 통합.
        """;
  }

  /** 입력이 일정 길이 이상이면 JSON 사전 요약(연관성·용어 정리) — 본 PRD의 user 메시지에만 첨부 */
  private String tryExtractStructuredBrief(Idea idea) {
    String original = sanitizeIdeaContentForPrd(nullToEmpty(idea.getContent()));
    if (!prdBriefEnabled || original.length() < prdBriefMinContentChars) {
      return null;
    }
    try {
      String raw =
          openAiHolder
              .getOpenAiChatService()
              .createChatCompletion(
                  prototypeModel,
                  List.of(
                      Map.of("role", "system", "content", briefExtractionSystemPrompt()),
                      Map.of("role", "user", "content", original)),
                  0.12,
                  3072);
      if (raw == null || raw.isBlank()) {
        return null;
      }
      String cleaned = stripJsonFence(raw.trim());
      JsonNode node = objectMapper.readTree(cleaned);
      if (node == null || !node.isObject()) {
        return null;
      }
      String pretty =
          objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
      log.info("PRD: 구조화 사전 요약 생성 ({}자)", pretty.length());
      return pretty;
    } catch (Exception e) {
      log.info("PRD: 구조화 사전 요약 생략 — {}", e.getMessage());
      return null;
    }
  }

  private String briefExtractionSystemPrompt() {
    return """
        Output **ONLY** valid JSON. No markdown fence, no explanation.
        All user-facing string values in **Korean** where it makes sense.
        Schema (use "" and [] for empty):
        {
          "productName": "",
          "oneLiner": "",
          "problem": "",
          "targetUsers": [""],
          "personas": [{"name":"","role":"","goal":"","pain":""}],
          "valuePropositions": [""],
          "mvpFeatures": [""],
          "nonGoals": [""],
          "assumptions": [""],
          "constraints": [""],
          "competitorsOrAlternatives": [""],
          "successMetrics": [""],
          "openQuestions": [""]
        }
        Infer carefully from the user message. If sparse, list assumptions as explicit (가정) in "assumptions".
        """;
  }

  /** 초안이 짧으면 동일 뼈대로 확장(2차 호출) */
  private String expandPrdIfThin(String draft) {
    if (draft == null || draft.length() >= prdMinCharsForExpand) {
      return draft;
    }
    try {
      String raw =
          chatExpand(
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      "시니어 PM입니다. 아래 PRD **초안**을 **삭제·축약하지 말고** `##` 구조·순서를 유지한 채 **빈약한 섹션을 채웁니다**. "
                          + "특히 `## 사용자 스토리`, `## 엣지 케이스·오류·예외`, `## MVP 정의 완료(DoD)`, `## 용어집`, `## 의존성·외부 시스템`이 "
                          + "짧으면 **표·체크리스트**로 충분히 보강하세요. "
                          + "기능 요구 표는 **최소 8행**, 사용자 스토리 P0 **최소 5행**. "
                          + "한국어 마크다운만. 내부 툴·LLM·파이프라인 언급 금지."),
                  Map.of("role", "user", "content", draft)));
      if (raw != null && !raw.isBlank() && raw.length() > draft.length()) {
        log.info("PRD: 확장 2차 pass 적용 ({} -> {}자)", draft.length(), raw.length());
        return normalizePrdMarkdown(raw.trim());
      }
    } catch (Exception e) {
      log.warn("PRD 확장 pass 생략: {}", e.getMessage());
    }
    return draft;
  }

  /**
   * 최종 편집: 실무 섹션이 누락·형편없을 때 보강(3차). 비용·지연을 줄이려면
   * application.yml에서 prototype-prd-editor-pass-enabled: false
   */
  private String prdEditorPassIfEnabled(String draft) {
    if (!prdEditorPassEnabled || draft == null || draft.isBlank()) {
      return draft;
    }
    try {
      String raw =
          openAiHolder
              .getOpenAiChatService()
              .createChatCompletion(
                  prototypeModel,
                  List.of(
                      Map.of("role", "system", "content", prdEditorSystemPrompt()),
                      Map.of("role", "user", "content", draft)),
                  0.22,
                  prdMaxTokens);
      if (raw == null || raw.isBlank()) {
        return draft;
      }
      String out = normalizePrdMarkdown(raw.trim());
      if (out.length() < 500 || !out.contains("##")) {
        log.warn("PRD: 편집 pass 결과가 비정상 — 원안 유지");
        return draft;
      }
      if (out.length() < (int) (draft.length() * 0.55)) {
        log.warn("PRD: 편집 pass가 과도하게 짧아 원안 유지");
        return draft;
      }
      log.info("PRD: 편집(실무 보강) pass ({} -> {}자)", draft.length(), out.length());
      return out;
    } catch (Exception e) {
      log.warn("PRD 편집 pass 생략: {}", e.getMessage());
      return draft;
    }
  }

  private String prdEditorSystemPrompt() {
    return """
        당신은 **Staff PM + 테크 리드**입니다. 아래 **PRD 초안 전체**를 **실제 스프린트·개발 착수**에 쓰일 수준으로
        **한국어 마크다운**으로 **다듬어 전부 다시 출력**하세요(부분 응답 금지).

        규칙:
        - 기존 `#`, `##` 구조는 **최대한 유지**하고, **빈약하거나 누락된 섹션은 내용을 채웁니다**. 임의로 절반 삭제·요약해 길이를 줄이지 **마세요**.
        - **반드시** 채울 섹션(없으면 `##` 제목을 추가, 이미 있으면 내용만 보강):
          - `## 사용자 스토리` — 표 | ID | As a | I want | So that | 수용 기준 |, P0 **5행 이상**
          - `## 엣지 케이스·오류·예외` — **6불릿 이상**
          - `## MVP 정의 완료(DoD)` — **체크 8개 이상**
          - `## 의존성·외부 시스템` — 없으면 "없음" 또는 TBD+영향
          - `## 용어집` — **5항목 이상** 표
        - 수용 기준·DoD는 **검증 가능**하게(측정·Given/When/Then 허용). 추측은 `(가정)`.
        - 금지: "파이프라인", "LLM", "OpenAI", "토큰", "Vercel", **내부 툴·생성기** 언급.
        - 응답은 **PRD 전문**만. 머리말·``` 로 전체를 감싸지 말 것.
        """;
  }

  private String buildPrdUserPrompt(Idea idea, String optionalBriefJson) {
    String original = sanitizeIdeaContentForPrd(nullToEmpty(idea.getContent()));
    KeywordAnalysis analysis = analyzeKeywords(original);
    String modeHint =
        analysis.keywordLike
            ? "[입력 특성] 짧은 조각·키워드가 많습니다. 연결 관계를 추론해 문장으로 풀어 쓰세요."
            : "[입력 특성] 문장 위주 메모입니다. 중복을 줄이고 한 제품으로 통합하세요.";

    if (log.isDebugEnabled()) {
      log.debug(
          "PRD user prompt: brief={} keywordLike={} tokenCount={} avgLen={}",
          optionalBriefJson != null,
          analysis.keywordLike,
          analysis.tokenCount,
          analysis.avgTokenLength);
    }

    StringBuilder sb = new StringBuilder();
    if (optionalBriefJson != null && !optionalBriefJson.isBlank()) {
      sb.append(
          "---\n[참고: 아래 JSON은 입력을 구조화한 내부 요약입니다. **PRD 본문에 JSON을 그대로 복사하지 마세요.** "
              + "논리가 충돌하면 **아이디어 원문**·일관된 제품 서사를 우선하세요.]\n");
      sb.append(optionalBriefJson);
      sb.append("\n---\n\n");
    }
    sb.append(modeHint);
    sb.append(
        "\n\n아이디어 원문(캔버스 카드·메모를 --- 로 이었을 수 있음). **아래 원문(및 위 참고)만** 근거로 PRD를 작성하세요.\n\n");
    sb.append(original);
    return sb.toString();
  }

  /** [PRD_PIPELINE] 등 내부 접두 제거 — 모델이 문서에 그대로 넣지 않도록 */
  private static String sanitizeIdeaContentForPrd(String content) {
    if (content == null || content.isBlank()) {
      return "";
    }
    return content.trim().replaceFirst("(?s)^\\[PRD_PIPELINE]\\s*", "");
  }

  /** LLM이 전체를 ```markdown 으로 감싼 경우 제거 */
  private static String normalizePrdMarkdown(String raw) {
    String s = raw.trim();
    if (s.startsWith("```")) {
      int firstNl = s.indexOf('\n');
      int endFence = s.lastIndexOf("```");
      if (firstNl > 0 && endFence > firstNl) {
        s = s.substring(firstNl + 1, endFence).trim();
      }
    }
    return s;
  }

  private static KeywordAnalysis analyzeKeywords(String text) {
    if (text == null || text.isBlank()) {
      return new KeywordAnalysis(false, 0, 0, List.of());
    }
    String[] raw = text.split("[\\n,|;/•·]+");
    List<String> tokens = new ArrayList<>();
    int totalLen = 0;
    for (String r : raw) {
      String t = r.trim();
      if (t.isEmpty()) {
        continue;
      }
      tokens.add(t);
      totalLen += t.length();
    }
    int count = tokens.size();
    int avg = count == 0 ? 0 : totalLen / count;
    boolean keywordLike = count >= 6 && avg <= 12;
    List<String> top = tokens.stream().filter(t -> t.length() >= 2).distinct().limit(12).toList();
    return new KeywordAnalysis(keywordLike, count, avg, top);
  }

  private record KeywordAnalysis(
      boolean keywordLike, int tokenCount, int avgTokenLength, List<String> topTokens) {}

  private static String stripJsonFence(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw.trim();
    if (s.startsWith("```")) {
      int start = s.indexOf('\n');
      int end = s.lastIndexOf("```");
      if (start > 0 && end > start) {
        return s.substring(start + 1, end).trim();
      }
    }
    return s;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
