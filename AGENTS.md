# AGENTS.md

AlwaysOnGlyph — Nothing OS의 **Glyph Matrix** 하드웨어를 활용해, 폰을 잠그고 뒤집었을 때(Flip-to-Glyph) LED 매트릭스에 시계·배터리·읽지 않은 알림 수를 표시하는 Always-on Glyph Toy 앱입니다.

## 주요 기능

1. **Flip-to-Glyph 매트릭스 시계 & 상태 표시**
   - 상단: 픽셀 폰트 `HHmm` 시계 (24시간/12시간 지원, 디지털/아날로그 스타일)
   - 하단: 상태 위젯 — 알림(`· N`), 배터리(`NN%`), 충전 전력(`NNW`), 온도(`NN°`), 날씨(`NN°`)
2. **상황 기반 위젯 우선순위(Priority) 모드**
   - 읽지 않은 알림 존재 시 알림, 충전 중엔 배터리/전력, 기본은 날씨 위젯 자동 표시
3. **Jetpack Compose 컨트롤 앱 & 라이브 프리뷰**
   - 25x25 매트릭스 출력 비트맵 실시간 픽셀 아트 프리뷰
   - Glyph Toy 매니저·알림 접근 권한 설정 바로가기
   - 설정: 밝기, AOD 끄기 예약(시간대/DND), 화면 켜짐 시 끄기, 위젯 편집

## 기술 스택

- Language: Kotlin (JVM 11)
- UI: Jetpack Compose + Material 3 (Compose BOM `2026.02.01`)
- SDK: Nothing `GlyphMatrix-Developer-Kit` v2.0 (`app/libs/glyph-matrix-sdk-2.0.aar`)
- targetSdk 36 / minSdk 35 / compileSdk 36
- 날씨: Open-Meteo 무료 API (위치: 기기 GPS 또는 IP 기반 fallback)

## 빌드 & 테스트

```bash
./gradlew assembleDebug      # 디버그 APK 빌드
./gradlew installDebug       # 연결된 기기에 설치
./gradlew test               # 단위 테스트
./gradlew assembleFullRelease assembleLiteRelease   # 릴리스 APK (CI와 동일)
```

제품 플레이버: `full`(알림 집계 포함, `HAS_NOTIFICATION=true`) / `lite`(알림 기능 제외).

## 프로젝트 구조

```
app/src/main/java/com/jh/alwaysonglyph/
├── MainActivity.kt                 # Compose 메인 화면 (라이브 프리뷰 + 설정)
├── EditWidgetsActivity.kt          # 상태 위젯 활성화/순서·알림 필터 편집
├── NotificationAppsActivity.kt     # 알림 집계할 앱 선택
├── AppList.kt                      # 런처 앱 목록 조회 유틸
├── prefs/
│   └── ClockPreferences.kt         # SharedPreferences 설정 (밝기·AOD 예약·위젯 등)
├── renderer/
│   ├── MatrixCanvasRenderer.kt     # 25x25 픽셀 렌더러 (도트 폰트 5x7/3x5, 아날로그)
│   ├── StatusWidget.kt             # 상태 위젯 enum + StatusWidgetModule(텍스트/순환)
│   └── WidgetPriority.kt           # 위젯 우선순위 스택 (PriorityList)
├── receiver/
│   └── BatteryStateReceiver.kt     # 배터리 잔량/온도/충전 전력 조회
├── service/
│   └── GlyphClockToyService.kt     # Nothing OS AOD Glyph Toy 서비스 (com.nothing.glyph.TOY)
├── weather/
│   └── WeatherRepository.kt        # Open-Meteo 날씨 조회 + 캐시
└── ui/theme/                       # Material 3 테마 (동적 컬러 지원)
app/src/full/                       # full 플레이버 (알림 리스너 서비스 + Manifest)
app/src/lite/                       # lite 플레이버 (NotificationAccess 스텁)
app/src/main/res/                   # 리소스 (strings, drawable, themes)
app/libs/glyph-matrix-sdk-2.0.aar   # Nothing Glyph Matrix SDK
app/src/test/                       # 단위 테스트 (MatrixCanvasRendererTest, WidgetPriorityTest)
.github/workflows/release.yml       # CI: 릴리스 APK 빌드·업로드
```

## 시스템 작동 원리

1. `AndroidManifest.xml`에 `com.nothing.glyph.TOY` Action + `aod_support=1` 메타데이터로 `GlyphClockToyService` 등록.
2. Nothing OS 설정에서 "Clock & Status Matrix" Toy 선택 → 폰을 뒤집으면 시스템이 `EVENT_AOD` 신호 전송.
3. 서비스가 `MatrixCanvasRenderer`로 1:1 비트맵 렌더링 후 `GlyphMatrixManager.setMatrixFrame(...)`으로 점등.
4. 알림 접근 권한이 없으면 배터리 퍼센트로 안전 fallback, 알림 발생 시 자동 `· N` 전환.

## 핵심 개념

- **Toy 식별자**: Nothing OS 설정에서 `Clock & Status Matrix`(`toy_name`)로 표시. `com.nothing.glyph.TOY` Action, `aod_support=1`/`longpress=1` 메타데이터.
- **대상 기기**: `Glyph.DEVICE_23112`(Nothing Phone (3), 25x25 LED 매트릭스) — `GlyphClockToyService.initGlyphManager()`에서 등록.
- **진입점 구분**: `MainActivity`는 컨트롤/설정 패널일 뿐이며, 실제 매트릭스 표시는 백그라운드 `GlyphClockToyService`가 담당한다.
- **시스템 이벤트** (`GlyphClockToyService.serviceHandler`):
  - `EVENT_AOD`: 폰 뒤집기 → 매트릭스 갱신
  - `EVENT_CHANGE`: Glyph 버튼 길게 누르기 → 위젯 순환(수동 모드) 또는 `WidgetPriority.advance()`(우선순위 모드)

## 런타임 데이터 흐름

```
상태 소스                          렌더러                        출력
BatteryStateReceiver  ─┐
  (배터리 %/온도/W)     │
WeatherRepository     ─┼─→ GlyphClockToyService.updateMatrixDisplay()
  (Open-Meteo, 30분 캐시)│     → MatrixCanvasRenderer.renderFrame()/renderAnalogFrame()
NotificationAccess    ─┘     → GlyphMatrixManager.setMatrixFrame(...)
  (읽지 않은 수; lite는 0)
```

- `WidgetPriority`는 앱 전역 싱글턴(`renderer/WidgetPriority.kt`)으로, `GlyphClockToyService`와 알림 리스너가 공유한다.
- `UnreadNotificationListenerService`(full 플레이버)가 알림 발생/제거 시 `WidgetPriority.push/remove(NOTIFICATION)`로 즉시 우선순위를 갱신한다.
- `BatteryStateReceiver`는 충전 시작/종료 시 `BATTERY`/`WATTAGE`를 push/remove한다(`syncChargingPriority()`).

## 위젯 우선순위 규칙

- **우선순위 모드**(`priority_enabled`, 기본 켜짐): 읽지 않은 알림 존재 시 `· N` → 충전 중 `NN%`/`NNW` → 기본 `NN°`(날씨) 자동 표시.
- **수동 모드**: 사용자가 선택한 위젯을 표시하고, `EVENT_CHANGE`(길게 누르기)로 순환.
- **알림 카운트 규칙** (`UnreadNotificationListenerService.updateUnreadCount`): `clearable`이면서 `ongoing`이 아닌 알림만 카운트. `notification_filter_enabled` 시 사용자가 선택한 앱만 집계.
- **우선순위 스택** (`PriorityList`): 마지막 push가 최우선 표시, `advance()`는 첫 요소 방향으로 순환·랩어라운드, 비면 `WEATHER` 표시.

## 주요 기본값/상수

- 기본 밝기 `200`(0–255), AOD 끄기 기본 시간대 `22:00–07:00`(자정 경유 지원).
- 날씨 갱신 주기 30분(`WeatherRepository.REFRESH_INTERVAL_MS`, `GlyphClockToyService.WEATHER_REFRESH_INTERVAL_MS`).
- 배터리/온도/전력은 sticky broadcast(`ACTION_BATTERY_CHANGED`)에서 조회, 충전 전력 = 전압(mV) × 전류(µA).

## UI 개발 가이드

UI는 Jetpack Compose + Material 3로 작성합니다. Material 3 컴포넌트·테마·레이아웃 구현 시 프로젝트에 등록된 **material-3** 스킬(`.opencode/skills/material-3/SKILL.md`)을 참고하세요. 기존 화면(`MainActivity.kt`, `EditWidgetsActivity.kt` 등)의 컨벤션(Scaffold + TopAppBar, `SettingsCard`/`SectionHeader`, `MaterialTheme.colorScheme` 토큰 사용)을 따릅니다.
