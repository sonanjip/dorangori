# Dorangori / 도란고리 (Android + Wear OS)
워치에서 음성을 녹음해 폰으로 보내고, 폰이 OndolSaem ASR로 텍스트로 바꿔 알림(복사 / 말벗다리에 묻기 / 조각보에 저장)으로 건네는 폰-워치 페어 앱. 이름은 "도란도란 + 고리"(도란: 낮은 목소리, 고리: 폰-워치 연결). 앞으로 폰-워치 페어가 필요한 온디바이스 AI 기능이 여기에 붙는다. 모델은 온디바이스 모델 호스트 OndolSaem(`../ondolsaem`) SDK로만 쓴다(워치에서 모델 실행 없음). 폰 앱은 INTERNET 권한이 없다.
applicationId `io.github.taehunkim.dorangori`(`:app` 폰과 `:wear` 워치가 **같아야** Data Layer가 둘을 짝짓는다), namespace는 `…dorangori` / `…dorangori.wear`. Kotlin + Compose(워치는 Wear Compose), 폰 minSdk 29 / 워치 minSdk 30 / target 36 / compile 37.

## 문서 유지 규칙
코드를 바꾸면 같은 커밋에서 관련 문서도 고친다(필요 없으면 이유 한 줄). 새 기능·화면·설정 → README(en/ko 기능 표), `docs/user-guide.md`; 구조·데이터·전송 → `docs/architecture.md`; 권한·네트워크·저장 데이터 → `docs/privacy.md`; 빌드·테스트 → `docs/development.md`; 계획·한계 → `docs/roadmap.md`; 의존성 → `THIRD_PARTY_NOTICES.md`. 문자열은 `values/strings.xml`(en)과 `values-ko/strings.xml`(ko) 키가 같아야 한다(모듈별 `StringsParityTest`).

## 스키마·버전
Room `@Entity` 변경 시 `AppDatabase.VERSION` 증가 + `Migrations.kt`(`ALL_MIGRATIONS`) + `app/schemas` JSON + 마이그레이션 테스트. destructive 폴백 금지(녹음·텍스트는 사용자 데이터). 앱 버전은 `app/build.gradle.kts`의 `appVersionName` 한 곳(`wear/build.gradle.kts`에도 같은 값, 함께 올린다).

## 환경·워크플로우
- `source scripts/env.sh`(JBR, SDK, adb, 폰/워치 AVD·시리얼). 폰 AVD `dori_phone36`(Play 스토어), 워치 AVD `dori_wear36`(Wear OS 6). 시리얼은 5554(폰)·5556(워치).
- `scripts/emu-start.sh [--window]`, `emu-stop.sh`, `run.sh [phone|wear|both]`, `logcat.sh [phone|wear] [N]`, `screenshot.sh [phone|wear]`.
- `local.properties`: `ondolsaem.dir=../ondolsaem`, `jogakbo.dir=../jogakbo`(composite build).
- `./gradlew assembleDebug testDebugUnitTest`. 여러 레포의 Gradle은 한 번에 하나씩 돌린다(메모리).
- 서명: Nunsori 공용 release 키를 `DORANGORI_STORE_FILE/PASSWORD`, `DORANGORI_KEY_ALIAS/PASSWORD` 속성으로 준다. 같은 키라야 OndolSaem·조각보가 승인 없이 받아 준다.

## 구조 (요약, 계획은 `docs/architecture.md`)
- 워치: 녹음(포그라운드 서비스, AAC m4a 16 kHz mono) → Data Layer `DataClient` + Asset으로 폰에 전송 → 폰이 DataItem을 지우면 워치 파일 삭제.
- 폰: 수신 → Room → ASR(OndolSaem) → 알림 + 목록 화면. 변환 시점(바로 / 예약 / 유휴 / 수동)과 자동 삭제는 설정.
