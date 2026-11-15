# 멜론 턴테이블 (안드로이드 앱)

## APK 만들기 (방법 A: Android Studio)
1. Android Studio에서 이 폴더 열기 → Gradle Sync
2. 메뉴 Build > Build Bundle(s) / APK(s) > Build APK(s)
3. 완료 알림의 locate를 누르면 `app/build/outputs/apk/debug/app-debug.apk`

## APK 만들기 (방법 B: GitHub Actions, 설치 불필요)
1. 이 폴더를 GitHub 저장소에 올리기 (루트에 build.gradle.kts, .github 폴더가 오도록)
2. Actions 탭 > Build APK > Run workflow
3. 끝나면 Artifacts에서 melon-turntable-debug-apk 다운로드

## 폰에 설치
APK를 폰으로 옮겨 실행 → '출처를 알 수 없는 앱 설치' 허용 → 설치.
처음 실행 시 알림 접근 권한에서 '멜론 턴테이블'을 켜면 멜론과 연동됩니다.
