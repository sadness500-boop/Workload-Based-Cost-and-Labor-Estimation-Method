# Workload-Based Cost and Labor Estimation Method

**업무량 기반 비용 및 인력 산정 방식** · 에스텍 그룹 · v1.3

용역계약(보안·미화·주차·시설·기타)을 "인당 견적"이 아니라 **업무량**으로 산정하는 프로그램입니다.

1. 사업장 유형 선택 — 공장 · 빌딩 · 병원 · 유통 · 대학교 · 연수원 · 발전소 · 아파트 · 기타
2. 직종 선택 — 보안 · 미화 · 주차 · 시설 · 기타
3. 업무를 대 · 중 · 소 · 세부로 분류하고 업무별 기본시간 입력
4. 업무량 → 월 업무시간 → 필요 인원(미화는 남 · 여 구분) → 인건비 · 견적
5. 견적 금액을 분류된 업무별 비용으로 배분 (합계는 견적과 원 단위까지 일치)

## 내려받기

| 구분 | 파일 | QR |
|---|---|---|
| 안드로이드 앱 | [Workload-Based_Cost_and_Labor_Estimation_Method_v1.3.apk](releases/Workload-Based_Cost_and_Labor_Estimation_Method_v1.3.apk) | `qr/qr-apk.png` |
| 웹 버전 | [index.html](index.html) (GitHub Pages) | `qr/qr-web.png` |

- **안드로이드 설치:** APK 내려받기 → "출처를 알 수 없는 앱 설치 허용" → 설치. Play 프로텍트 경고가 뜨면 "무시하고 설치".
- **웹 버전 주소:** https://sadness500-boop.github.io/workload-based-cost-and-labor-estimation-method/
  (저장소 Settings › Pages › Branch를 `main` / `/(root)` 로 켜야 열립니다)

## 폴더 구성

```
index.html                  웹 버전 (GitHub Pages 시작 화면)
releases/                   버전별 배포 파일 (APK · HTML)
android/                    안드로이드 앱 소스 (WebView 래퍼)
qr/                         설치용 QR 코드
```

## 산정 전제

2026년 최저시급 10,320원, 월 유급시간 = (주 소정근로 + 주휴) × 4.345주, 사업주 부담 4대보험 2026년 요율 기본값(산재는 업종별 직접 입력).
예시 업무 · 표준시간 · 기본규모는 설명용이므로 현장 실측값으로 바꿔 사용하세요.

---

*Produced by RatRae Choi – October 21, 2026*
