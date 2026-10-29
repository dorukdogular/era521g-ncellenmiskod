# ynusuncode (15.05.2026 - Robotta En Son Koşan Kararlı Sürüm)

Takım No: 10521
WPILib Sürümü: 2026.2.1

Bu proje WPILib VS Code ortamında doğrudan 'Open Folder' ile açılabilir, `./gradlew build` ile derlenebilir ve `./gradlew deploy` ile robota yüklenebilir.

## Güncelleme: Limelight, shooter PID, anti-jam
Atölyede [ATOLYE_REHBERI.md](ATOLYE_REHBERI.md) içindeki üç iş yeter: Limelight filtreleri, yön, atış tablosu. Pratikte `Constants.TUNING_ENABLED = true`, yarışmada `false`.
Birim testler: `./gradlew test` (AntiJam, BandLatch, sabit kontrolleri). Gradle için WPILib JDK'sı gerekir: `export JAVA_HOME=~/wpilib/2026/jdk`.
