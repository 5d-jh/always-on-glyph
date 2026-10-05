# AlwaysOnGlyph - Nothing OS Flip-to-Matrix Status Clock

Nothing OS의 **Glyph Matrix** 하드웨어를 활용하여, 유저가 폰을 잠그고 뒤집었을 때(**Flip to Glyph**) LED 매트릭스에 시계와 배터리 잔량 / 읽지 않은 알림 수를 표시해 주는 **Always-on Glyph Toy** 애플리케이션입니다.

---

## 📱 프로젝트 주요 기능

1. **Flip-to-Glyph 매트릭스 시계 & 상태 표시 (Nothing OS 연동)**
   * **상단 영역**: 대형 픽셀 폰트로 출력되는 `HHmm` 24시간제 시계 (예: `1430`)
   * **하단 영역**:
     * **기본 상태**: 배터리 잔량 퍼센트 (예: `79%`)
     * **읽지 않은 알림 존재 시**: 가운데점 + 읽지 않은 알림 수 (예: `· 3`)
2. **다양한 매트릭스 해상도 자동 대응**
   * **Phone (3)**: 25x25 LED 매트릭스
   * **Phone (4a) Pro**: 13x13 LED 매트릭스
3. **Jetpack Compose 컨트롤 및 라이브 프리뷰 앱**
   * 앱 실행 시 25x25 매트릭스 실제 출력 비트맵을 화면에서 실시간 픽셀 아트 프리뷰로 확인 가능.
   * Nothing OS **Glyph Toy 매니저 설정 바로가기** 제공 (`Settings > Glyph Interface > Flip to Glyph > Always-on Glyph Toy`).
   * **알림 접근 권한 설정 바로가기** 제공.

---

## 🛠 기술 스택 및 라이브러리

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (Material3)
* **Target SDK**: Android 36 (Min SDK 35, Java 11)
* **SDK Integration**: Nothing `GlyphMatrix-Developer-Kit` v2.0 (`app/libs/glyph-matrix-sdk-2.0.aar`)

---

## 📂 프로젝트 구조 (Project Directory)

```
app/src/main/
├── java/com/jh/alwaysonglyph/
│   ├── MainActivity.kt                       # Compose 메인 UI (Live Preview, 설정 바로가기)
│   ├── renderer/
│   │   └── MatrixCanvasRenderer.kt           # 25x25 및 13x13 픽셀 매트릭스 렌더러 (도트매트릭스 폰트 5x7, 3x5, 2x6, 2x5)
│   ├── receiver/
│   │   └── BatteryStateReceiver.kt           # 배터리 퍼센트 감지 수신기
│   └── service/
│       ├── GlyphClockToyService.kt           # Nothing OS AOD Glyph Toy 서비스 (com.nothing.glyph.TOY)
│       └── UnreadNotificationListenerService.kt # 읽지 않은 알림 수 집계 (NotificationListenerService)
├── libs/
│   └── glyph-matrix-sdk-2.0.aar              # Nothing Glyph Matrix Developer Kit v2.0
└── res/
    └── values/strings.xml                    # Toy 이름 및 설명 리소스
```

---

## ⚙️ 시스템 작동 원리 (Architecture)

1. `AndroidManifest.xml`에 `com.nothing.glyph.TOY` Action 및 `aod_support = 1` 메타데이터로 `GlyphClockToyService`가 등록되어 있습니다.
2. 사용자가 Nothing OS 설정(`Glyph Interface > Flip to Glyph > Always-on Glyph Toy`)에서 **"Clock & Status Matrix"**를 토이로 선택합니다.
3. 사용자가 폰을 잠그고 엎어놓으면(Flip), Nothing OS의 시스템 엔진이 `GlyphClockToyService`로 `GlyphToy.EVENT_AOD` 신호를 보냅니다.
4. 서비스 수신 시 `MatrixCanvasRenderer`가 1:1 비트맵을 렌더링하고 `GlyphMatrixManager.setMatrixFrame(...)`을 통해 하드웨어 매트릭스를 점등시킵니다.
5. 알림 접근 권한이 없는 경우 배터리 퍼센트로 안전하게 fallback 되며, 읽지 않은 알림이 생기면 자동으로 `· N` 형태로 전환됩니다.

---

## 🚀 빌드 및 실행 가이드 (Build & Deployment)

### 1. 의존성 확인
* `app/libs/glyph-matrix-sdk-2.0.aar` 파일이 존재하는지 확인합니다.
* `app/build.gradle.kts`에 아래 설정이 포함되어 있습니다:
  ```kotlin
  dependencies {
      implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
  }
  ```

### 2. 빌드 및 설치 명령
```bash
# Debug APK 빌드
./gradlew assembleDebug

# 연결된 Android / Nothing 기기에 앱 설치
./gradlew installDebug

# 단위 테스트 실행 (Pixel Format & Logic Test)
./gradlew test
```

---

## 🔗 See Also

* **Nothing Glyph Matrix Developer Kit**: [https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
