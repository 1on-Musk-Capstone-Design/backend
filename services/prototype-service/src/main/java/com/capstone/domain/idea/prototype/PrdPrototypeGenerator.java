package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import org.springframework.stereotype.Component;

@Component
public class PrdPrototypeGenerator {

  public String generate(Idea idea) {
    String title = summarizeTitle(idea.getContent());
    String body = escapeBlock(idea.getContent());
    return """
        # """
        + title
        + """

        ## 문서 메타
        | 항목 | 내용 |
        | --- | --- |
        | 버전 | 0.1 (템플릿·API 미사용) |
        | 문서 | PRD |
        | 상태 | 초안·보완 필요 |

        ## 한 줄 요약(Executive summary)
        OpenAI가 비활성이거나 API 오류로 **LLM PRD를 생성할 수 없을 때**의 폴백 문서입니다. 팀이 동일 뼈대(PRD 섹션)로 내용을 채우도록 합니다. 실서비스 수준 PRD는 `OPENAI_API_KEY`와 프로토타입 옵션이 켜진 뒤 PRD/프로토타입 생성을 다시 실행하세요.

        ## 문제 정의
        - (아래 **원본 메모**를 근거로 pain·AS-IS·기회를 문장으로 풀어 적으세요.)

        ## 제품 비전·목표
        - (정성·측정 KPI를 각 1~2문장)

        ## 대상 사용자·페르소나
        - (역할/목표/고충, 1~2인)

        ## 핵심 사용 시나리오
        - (3개 이상, 트리거→단계→결과)

        ## 사용자 스토리
        | ID | As a | I want | So that | 수용 기준 |
        | --- | --- | --- | --- | --- |
        | US-1 | (채움) | (채움) | (채움) | (검증 가능) |

        ## 기능 요구사항
        | ID | P0~P2 | 요구 | 가치 | 수용 기준 |
        | --- | --- | --- | --- | --- |
        | F-1 | P0 | (채움) | (채움) | (채움) |

        ## 엣지 케이스·오류·예외
        - (입력 누락, 권한, 네트워크, 빈 상태 등)

        ## 비기능 요구사항
        - (보안·성능 SLO TBD·접근성·로그)

        ## 범위 밖(Non-goals)
        - (이번에 하지 않을 것)

        ## 가정·제약
        - (가정) (제약)

        ## 의존성·외부 시스템
        - (없음 또는 TBD)

        ## MVP 정의 완료(DoD)
        - [ ] 핵심 플로우
        - [ ] 에러·권한
        - (항목 추가)

        ## 용어집
        | 용어 | 정의 |
        | --- | --- |
        | (채움) | (채움) |

        ## 리스크·완화
        - (리스크 / 완화)

        ## 롤아웃·릴리스
        - (MVP 범위·단계)

        ## 열린 질문
        - (5개 이상)

        ## 원본 메모(입력)
        """
        + body
        + """

        ## 다음 단계
        - API 키·프로토타입 생성을 켜고 PRD를 재생성하거나, 위 섹션을 팀이 직접 보강하세요.
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
