package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import org.springframework.stereotype.Component;

@Component
public class PrdPrototypeGenerator {

  public String generate(Idea idea) {
    String title = summarizeTitle(idea.getContent());
    String body = escapeBlock(idea.getContent());
    return """
        # PRD: """
        + title
        + """

        ## 1. 배경
        캔버스에 등록된 아이디어를 바탕으로 자동 생성된 제품 요구사항 문서입니다.

        ## 2. 문제 정의
        - 사용자가 아이디어를 빠르게 시각화·검증할 수 있도록 프로토타입이 필요합니다.

        ## 3. 목표
        - 아이디어 내용을 반영한 단일 페이지 React 프로토타입을 생성합니다.
        - (선택) GitHub 저장소 및 Vercel 배포로 공개 URL을 제공합니다.

        ## 4. 범위 (MVP)
        - 랜딩/소개 섹션 1개
        - 핵심 가치 제안 3가지
        - CTA 버튼 1개

        ## 5. 비기능 요구사항
        - 반응형 레이아웃
        - 접근성: 시맨틱 태그 사용

        ## 6. 원본 아이디어
        """
        + body
        + """

        ## 7. 승인
        - 본 문서는 파이프라인 실행 시 자동 생성되었습니다.
        """;
  }

  private static String summarizeTitle(String content) {
    if (content == null || content.isBlank()) {
      return "Untitled Idea";
    }
    String oneLine = content.replace('\n', ' ').trim();
    return oneLine.length() > 48 ? oneLine.substring(0, 48) + "…" : oneLine;
  }

  private static String escapeBlock(String content) {
    if (content == null) {
      return "(내용 없음)";
    }
    return content;
  }
}
