# Atölye Rehberi (tek gecelik test için)

Takım 10521 | WPILib 2026.2.1 | REVLib 2026.0.5 | Limelight 2 (LL OS 2026.0)

Bu dosya, yazılımı robotta ilk kez denerken sırayla izlenecek listedir. Kodun **donanımda test edilmediğini** unutma: `./gradlew build` ve 23 birim testi geçiyor, ama motor/kamera davranışı ilk kez bu gece görülecek. Aşağıdaki sıra, riski en düşükten en yükseğe doğru gidiyor. **Zaman yetmezse 1-6'yı bitir, 7-10'u sonraya bırak.**

---

## 0. Başlamadan önce (5 dk)

**Yazılım**
- [ ] Gradle'ı WPILib'in JDK'sı ile çalıştır. VS Code'un WPILib uzantısı bunu kendisi yapar. Terminalden: `export JAVA_HOME=~/wpilib/2026/jdk`.
- [ ] Test gecesi için `Constants.java` içinde `TUNING_ENABLED = true` yap. Bu açıkken dashboard'dan şu değerler canlı ayarlanır: `Shooter/TargetRPM`, `Shooter/kP`, `Shooter/kV`, `Align/kP`, `Align/kD`, `Align/MaxTurn`, `Align/MinTurn`, `Vision/AimOffsetDeg`, `Intake/P/I/D/OpenDegrees/ClosedDegrees`, `Feeder/Speed`, `Intake/RollerSpeed`, `Hood/ManualDegrees`, `Hood/kP`. Hepsi güvenli aralığa sıkıştırılır (clamp).
- [ ] Dashboard'da (Elastic kurulu: `~/wpilib/2026/elastic`) şu anahtarları ekranına koy:
  - Shooter: `Running, CurrentRPM, ActiveTargetRPM, Ready, Fault, Current, AppliedVolts, ImpliedKV, Temp`
  - Feeder: `Running, Current, Jam, JamCount, Temp`
  - Intake: `RollerRunning, RollerCurrent, RollerJam, RollerJamCount, Position, Setpoint, AtSetpoint, PivotCurrent`
  - Drive: `LeftCurrent, RightCurrent, LeftAppliedOutput, RightAppliedOutput`
  - Vision: `Alliance, HasTarget, RawTagId, PipelineOk, ActivePipeline, DataFresh, Tx, Ty, Distance, TableRpm, LatencyMs`
  - Align: `Aligned, Turn` | Shoot: `State`
- [ ] **Test sonunda bütün değerleri not al. Dashboard'dan girdiğin değerler robot kapanınca KAYBOLUR.** Kalıcı hale getirmek için `Constants.java`'ya yaz.

**Donanım / elektrik**
- [ ] Akü dolu (>12.5 V). Sahada da böyle olacak, test ederken de.
- [ ] **PDH'de roller (ID5), feeder (ID8) ve shooter (ID7) kanallarındaki breaker boyutunu kontrol et.** 40A değilse `IntakeRollerConstants.CURRENT_LIMIT`, `FeederConstants.CURRENT_LIMIT`, `ShooterConstants.CURRENT_LIMIT` değerlerini breaker'a göre düşür. (`AntiJamConstants.JAM_CURRENT_AMPS` da limitin ~%80'i olmalı, birim test bunu kontrol eder.)
- [ ] Anderson/CAN/PDH konnektörlerini çek-salla kontrolü. "Sahada güçsüz" şikayetinin en sık sebebi gevşek bir konnektör ya da yıpranmış akü.
- [ ] Top sıkışması çoğu zaman **mekaniktir**: kayış gerginliği, rulo sıkıştırması, intake ile hopper arasındaki geçiş. Yazılım sıkışmayı geri iterek açar ama mekanik sorunu çözmez.
- [ ] **Robot AÇILIRKEN intake kapalı konumda olmalı.** Pivot enkoderi açılışta 0 kabul edilir. Açık açarsan pivot yanlış referansla sert limite gider.
- [ ] İlk testler için robotu **tekerlekleri havada** blokların üstüne al.

---

## Kumanda haritası (tüm tuşlar)

Tek kontrolcü (port 0, Xbox). Tuşlar A, B ve D-pad için 0.06 s debounce var.

| Tuş | Ne yapar |
|---|---|
| **Sol çubuk Y** | İleri / geri sürüş (arcade). Eskiden olduğu gibi işaret çevrilmedi, ileri itince değer negatif gelir |
| **Sağ çubuk X** | Dönüş (arcade). Çubuk bırakılınca **0.08 deadband** içinde robot durur |
| **A** | Intake roller aç/kapa. **Sadece pivot açıkken** başlar; pivot kapanırsa roller kendiliğinden durur |
| **B** | Shooter aç/kapa (manuel ön ısıtma, `Shooter/TargetRPM` hızında). Disable'da kapanır |
| **D-pad (herhangi bir yön)** | Intake pivot aç/kapa (0° ↔ 90°) |
| **Sağ tetik (>0.5) veya Sağ bumper** (basılı tut) | **Manuel atış**: shooter manuel RPM'e çıkar, hazır olunca besler. Hizalama yok |
| **Sol tetik (>0.5) veya Sol bumper** (basılı tut) | **Otomatik nişan + atış**: robot hub tag'ine döner, shooter RPM'e çıkar, **hazır ve hizalı** olunca besler. Sol çubuk Y ile ileri/geri sürüş devam eder |
| Kontrolcü titreşimi | Hizalı **ve** shooter hazırken otomatik titrer |
| Diğer tuşlar (Start, Back, X, Y, çubuk basma) | Atanmamış |

**A, B ve D-pad atış sırasında basılırsa atışı artık iptal etmez** (eskiden iptal ediyordu).

### Atış sırasında ne olur (sağ ve sol tetik aynı)
1. **SPINNING_UP**: tetiğe basılınca pivot kapanır; shooter hedef RPM'e çıkar, feeder ve roller **durur**.
2. (Sadece sol tetik) **WAITING_AIM**: shooter hazır ama robot henüz hizalı değil, feeder bekler.
3. **SHOOTING** (shooter hazır + gerekirse hizalı): feeder çalışır, **pivot 0° ↔ 90° arasında yukarı-aşağı salınır ve topları feeder'a geri iter** (pivot hedefine varınca veya en geç 0.45 s'de yön değiştirir), roller pivot **açıkken** yardım eder.
4. RPM çok düşerse (hedef ±350 rpm dışı) veya hizayı kaybederse feeder ve roller durur, **pivot salınımı da durur ve pivot olduğu yerde kalır**; tekrar hazır olunca pivot açılarak salınım yeniden başlar.
5. Tetik bırakılınca: feeder ve roller durur, pivot kapanır. Shooter, **B ile önceden açılmadıysa** durur; açıktıysa manuel RPM'de dönmeye devam eder.

Test: tetiğe basılıyken `Intake/Position` 0 ↔ 90 arasında gidip gelmeli ve `Shoot/State` = `SHOOTING` olmalı.

---

## 1. Disable/enable güvenliği (2 dk)

1. Robotu enable et, **B** ile shooter'ı aç, dönsün. Sonra **disable** et ve tekrar **enable** et.
2. Beklenen: shooter **kendiliğinden dönmez**. Aynısını **A** (roller) ile dene (intake açıkken).

Dönerse dur ve bana bildir, bu bir hata.

---

## 2. Shooter yönü ve enkoder işareti (5 dk) ⚠️ ilk açılış

Eski kod `-1.0` güç veriyordu. Yeni kod `inverted(true)` + **pozitif RPM** kullanıyor ve fiziksel yön aynı olmalı.

1. Dashboard `Shooter/TargetRPM` = 2500 (en düşük izinli). Yakında dur, topsuz.
2. **B**'ye bas.
3. Kontrol et:
   - Flywheel **eskiden döndüğü yönde** mi dönüyor? (Ters ise dur, bana bildir.)
   - `Shooter/CurrentRPM` **pozitif** mi?
4. `Shooter/Fault` true olursa: enkoder ters okuyor demektir. Kod, RPM `-300`'ün altına inip 0.3 s kalırsa shooter'ı otomatik durdurup `Fault` verir (PID'nin kaçmasını önleyen emniyet). Bu olursa bana bildir, çözümü ben yaparım.

> Kaynak notu: NEO'nun dahili enkoderinin motor inversion'ını takip ettiği topluluk kaynaklarından biliniyor, REV'in resmi dokümanında net yazmıyor. Bu yüzden bu test ve `Fault` emniyeti var.

---

## 3. Shooter PID ayarı (20 dk)

**Amaç:** `CurrentRPM` hedefe oturmalı ve `Ready` true olmalı.

### 3a. kV (feedforward)
`kV` = `12 / 5676 = 0.002114 V/rpm` NEO'nun **yüksüz** serbest hızından hesaplandı. Gerçek flywheel yük altında biraz yavaş döner, yani gerçek kV **daha büyüktür**. kV eksik kalırsa RPM hedefin altında takılır ve **`Ready` hiç gelmez**.

1. `Shooter/kP` = 0 yap. `Shooter/TargetRPM` = 3000, **B** ile aç, 3-4 s bekle.
2. `Shooter/CurrentRPM` ve `Shooter/ImpliedKV` (uygulanan volt ÷ rpm) değerlerini oku.
3. `CurrentRPM` hedefin **>100 rpm altında** ise `Shooter/kV`'yi `ImpliedKV`'ye eşitle, tekrar bak. 4000 ve 5000 hedeflerinde tekrarla. Üç noktada da hedefe ±100 içinde oturana kadar sürdür.
4. Çıkan kV'yi `ShooterConstants.KV`'ye kalıcı yaz. (Dashboard aralığı `KV × 0.7 … 1.5`.)

### 3b. kP
1. `Shooter/kP`'yi 0.0002'den başla (aralık `0 … 0.002`). **kP'nin birimi REV dokümanında belirtilmiyor**, bu yüzden küçük adımlarla (0.0002 → 0.0004 → 0.0008) artır.
2. Her adımda hedefi 3000 ↔ 5000 arasında değiştirip izle: RPM hedefe **aşırı geçmeden** ve **titremeden** oturmalı.
3. Titreme/aşırı geçme görürsen bir önceki değere dön. Değeri `ShooterConstants.DEFAULT_KP`'ye yaz.

### 3c. Top yükü altında
Topla ateş et (sağ tetik). Her topta RPM biraz düşer ve toparlanır.
- [ ] `Shooter/Ready` top çıkışında **titremiyor** (düşüş 350 rpm'i geçmedikçe true kalır).
- [ ] `Shooter/Current` 40 A'yı geçmiyor.
- [ ] `Shooter/Temp` sürekli atışta ~70 °C'yi aşmıyor (aşıyorsa dur).

---

## 4. Intake pivot (5 dk)

Pivot açma/kapama **eskiden çalışıyordu**, mantığına dokunmadım. Tek farklar: eski `setReference` yerine `setSetpoint` (aynı komut, yeni isim) ve dashboard'dan girilen değerlere sınır (sadece `TUNING_ENABLED` açıkken okunur). Burada sadece "hâlâ eskisi gibi mi" diye bak.
- [ ] **POV** ile aç/kapa: `Intake/Position` 0 ↔ 90 arasında gidiyor, `AtSetpoint` true oluyor, `PivotCurrent` 30 A'yı geçmiyor.
- [ ] Hareket **sert çarpmadan** duruyor. Çarpıyorsa `Intake/P`'yi düşür veya `OUTPUT_MAX` (0.8) değerini 0.6'ya çek.
- [ ] Atış sırasında pivot salınımı (0 ↔ 90) mekanik zarar vermiyor (detay: aşağıdaki "Atış sırasında ne olur").

---

## 5. Roller / feeder anti-jam (15 dk) ⭐ ana şikayet

Önce **normal çalışmada** akımı ölç, sonra **sıkıştır**:

1. Intake'i aç (A), 1 top al. `Intake/RollerCurrent` normal değeri not et (örn. ~12-18 A).
2. 2-3 top birden vermeyi dene. Akım yükselir ama roller **dönmeye devam ediyorsa** bu sıkışma sayılmaz (`Jam` false kalmalı).
3. Rulo gerçekten **duruyorsa** (elle tutarak/sıkıştırarak): akım `~33 A`'ya çıkar, ~0.2 s sonra `Jam` true olur, roller 0.2 s **ters** döner ve tekrar ileri gider. `JamCount` artar.
4. Aynısını feeder için yap (sağ tetik).

**Eşik ayarı (`AntiJamConstants`)**
| Sorun | Çözüm |
|---|---|
| Sıkışıyor ama `Jam` hiç true olmuyor | Dashboard'da sıkışma anındaki en yüksek akımı oku. 33 A'nın altında kalıyorsa `JAM_CURRENT_AMPS`'i düşür (örn. 28) |
| Normal dönerken `Jam` yanıp sönüyor | `JAM_CURRENT_AMPS` yükselt veya `JAM_MAX_RPM` düşür |
| Ters itme topu fırlatıyor/yetmiyor | `REVERSE_POWER` (0.6) ve `REVERSE_SECONDS` (0.2) |
| 3 s'de 3 sıkışma sonrası 0.5 s duruyor | Bu bir koruma: motor ısınmasın diye |

> Gerçek top ile yapılmış bir kalibrasyon yok. Bu değerler başlangıç noktası, **bu testin amacı bu eşikleri ayarlamak**.

---

## 6. Drive (10 dk)

- [ ] Joystick bırakılınca robot kıpırdamıyor (deadband 0.08). Fazla/az gelirse `DriveConstants.DEADBAND`.
- [ ] Drive eskisi gibi sürülüyor. (Ek bir akım koruması eklemedim, secondary limit **yok**. Smart limit 50 → 40 A oldu; REVLib dokümanı bunun brushed motorlarda etkisiz olabileceğini söylüyor, yani sürüş büyük ihtimalle eskisiyle aynı.)
- [ ] İstersen robotu duvara iterek `Drive/LeftCurrent` değerine bak ve not et (dashboard sadece lider motorların akımını gösterir). Sahada mekanizmalar güçsüz kalıyorsa ve bu akım çok yüksekse bana söyle.

---

## 7. Limelight kurulumu (30 dk)

### 7a. Montaj ve kablolama (resmi LL2 Quick-Start'a göre)
- Kamerayı **titreşimsiz ve sağlam** monte et (vidalı, plastik pul). Mekanik gevşeklik açıyı bozar.
- Kamera **shooter ile aynı yöne** bakacak ve hub'ı görecek.
- Güç: PDH'den **2 adet 18-20 AWG kablo**, **5A breaker** ile. **VRM'e veya radyo güç modülüne bağlama.** Giriş aralığı 4.1-16 V, LL2 maksimum 13 W.
- Ethernet: kameradan **radyoya** twisted Cat6 (stranded) kablo, **gerilim koruması (strain relief)** ile.
- İsteğe bağlı pasif PoE kullanma. Standart 802.3 PoE cihazı **bağlama**.

### 7b. Yazılım yükleme (LL OS 2026.0)
> **Limelight 2 için en son sürüm 2026.0'dır.** 2026.1 yalnız LL3/3G/4 için çıktı. `LimelightHelpers` v1.14 "LLOS 2026.0 veya üstü" ister, yani 2026.0 yeterli.
1. **Önce eski pipeline/script'leri yedekle**, yükleme hepsini siler.
2. Kamerayı **kapat**, **microUSB ↔ USB** ile bilgisayara bağla (kendiliğinden flash moduna girer).
3. **Limelight Hardware Manager** → "Flash OS" sekmesi → `limelight2_2026_0.zip` imajını seç → cihazı seç → "Flash Device".
4. İşlem bitince USB'yi çıkar. (Flash modunda web arayüzü açılmaz.)

### 7c. Ağ (resmi sayfaya göre)
1. Bilgisayarı robot ağına bağla. Hardware Manager'dan cihaza çift tıkla ya da `http://limelight.local:5801`.
2. **Settings** sekmesi → takım numarası **10521** → "Update Team Number".
3. **IP Assignment = Static**: IP `10.105.21.11`, Netmask `255.255.255.0`, Gateway `10.105.21.1` → Update → **robotu kapat-aç**.
4. Artık: ayar paneli `10.105.21.11:5801`, kamera akışı `10.105.21.11:5800`. Durum LED'i **sabit sarı** = statik IP.
5. **Hostname `limelight` kalmalı.** Kod `VisionConstants.LIMELIGHT_NAME = "limelight"` kullanıyor. Değiştirirsen sabiti de değiştir.

### 7d. Pipeline'lar ⚠️ kod buna bağlı
Kod alliance'a göre **pipeline seçiyor**. İki AprilTag pipeline'ı oluştur:
| Pipeline | Tür | Standard sekmesi → ID Filters |
|---|---|---|
| **0** | AprilTag (36h11) | `2,4,5,10` (kırmızı hub) |
| **1** | AprilTag (36h11) | `18,20,21,26` (mavi hub) |

**Neden sadece bu ID'ler?** WPILib'in 2026 saha dosyasına göre hub'ın her yüzünde 2 tag var: biri yüzün **tam ortasında**, diğeri **0.355 m yanda**. Yandakine nişan alırsan 3 m'de ~6.8° sapar. Bu yüzden sadece ortadakiler. Kırmızı hub ID'leri 2-5 ve 8-11, mavi 18-21 ve 24-27; bunlardan ortadakiler yukarıdaki listedir.

> **Neden NetworkTables'tan filtre göndermedim?** `SetFiducialIDFiltersOverride` / `fiducial_id_filters_set` Limelight'ın dokümanına göre **sadece localization'ı** filtreler, tx/ty hedeflemesini değil. tx/ty'yi **pipeline'ın ID Filters** kutusu filtreler.

### 7e. Kamera ayarları (iki pipeline'da da aynı)
- **Pipeline türü AprilTag, 2D kullanıyoruz** (3D gerekmez). Tag boyutu ayarı 3D için önemlidir, 2D tx/ty'yi etkilemez. FRC için `165.1 mm` (6.5 in) kullanılır; 2026 sahası için oyun kılavuzundan **doğrula**.
- **Black Level = 0.**
- **Çözünürlük/FPS:** LL2'de AprilTag hızı **düşüktür** (resmi tablo: 1280×960'ta ~6 fps). Hizalama için en az **~15 fps** hedefle: arayüzdeki FPS'i izle ve gerekirse çözünürlüğü düşür veya "Detector Downscale"i artır. (Daha yüksek downscale = daha hızlı ama biraz daha kısa menzil.)
- **Pozlama/gain:** hareket bulanıklığı olmayacak kadar **kısa pozlama** + gain'i yükselt (Quick-Start önerisi gain ~15). Tag'ler tutarlı algılanana kadar ayarla. Atölyenin ışığı ile saha ışığı farklıdır, sahada yeniden bak.
- **LED'ler:** kod `ForceOff` yapar (tag mat baskı, LED sadece parlama yapar).
- ❌ **"Crosshair Calibrate" kullanma.** `ty` referansı crosshair'e bağlıdır ve RPM tablosu bozulur. Crosshair varsayılan (merkez) kalsın. Kameranın nişan eksenini `AIM_OFFSET_DEGREES` ile düzelteceğiz.
- Kamera akışında hub tag'i algılanmış görünmeli ve `Vision/RawTagId` doğru ID'yi göstermeli.

### 7f. Kamera eğimi
Kameranın dikey görüş alanı ±24.45°. Hedef (tag merkezi 1.124 m) hem yakın (1.5 m) hem uzak (5 m) mesafede kadrajda kalmalı. Kamera yüksekliği `h` ise, kadrajın ortası ~3 m'ye denk gelecek eğim: `eğim ≈ atan((1.124 − h) / 3)`.
| Kamera yüksekliği | Önerilen eğim | `ty` @1.5 m | @3 m | @4.5 m |
|---|---|---|---|---|
| 0.45 m | ~13° | +11° | 0° | −4.5° |
| 0.55 m | ~11° | +10° | 0° | −3.7° |
| 0.65 m | ~9° | +8.5° | 0° | −3.0° |

Eğim hassas ölçülmek zorunda **değil**: RPM tablosu mesafeyi değil doğrudan `ty` açısını kullanır. (1° eğim hatası 3 m'de ~0.26 m mesafe hatası yapardı, bu yüzden tabloyu mesafeye değil `ty`'ye bağladım.) Yine de eğimi **sabitle**, atış günü bir daha oynama.

---

## 8. Limelight doğrulaması (10 dk, robot sabit)

Alliance'ı `Vision/AllianceOverride`'dan KIRMIZI seç, kırmızı hub'ın **ortadaki tag'ini** (2, 4, 5 veya 10) kameraya göster:
- [ ] `Vision/PipelineOk` true, `ActivePipeline` = 0
- [ ] `Vision/HasTarget` true, `RawTagId` ∈ {2,4,5,10}
- [ ] Bir **yandaki** tag'i (3, 9 vb.) göster: `HasTarget` **false** olmalı (pipeline filtresi doğru çalışıyor).
- [ ] Alliance'ı MAVİ yap: `ActivePipeline` = 1 olmalı ve mavi ID'ler (18,20,21,26) algılanmalı, kırmızılar algılanmamalı.
- [ ] `Vision/DataFresh` true. Kamerayı kapatınca ~0.5 s sonra false olup `HasTarget` düşmeli.
- [ ] `Vision/LatencyMs` değerini not et (LL2'de 100-250 ms olabilir, hizalama PID'si buna göre düşük kazançta).
- [ ] Hedef sağdayken `Vision/Tx` **pozitif** mi?
- [ ] Menzil testi: **en uzak atış mesafende** (örn. 4.5 m) tag **güvenle** algılanıyor mu? Algılanmıyorsa pozlamayı/çözünürlüğü artır veya downscale'i düşür.

---

## 9. Hizalama (15 dk)

1. **Yönü doğrula:** robot tekerlekler havada. Tag kameranın **sağında** iken **sol tetik**: robot **saat yönünde** (sağa) dönmeli. Tersine dönüyorsa `AlignConstants.TURN_SIGN = -1.0`.
2. **MIN_TURN ölç:** tuning açık, robotu yere indir. `Align/MinTurn`'ü 0.05'ten başlayıp artır: robotun dönmeye başladığı ilk değeri bul, **üstüne +0.03 ekle**. Bu değeri `MIN_TURN_OUTPUT`'a yaz. (Çok düşük = hedefin yakınında takılı kalır, çok yüksek = sağa-sola titrer. Simülasyonda doğrulandı.) Min güç bant kenarına yaklaştıkça `MIN_TURN_FRACTION` (0.7) oranına kadar yumuşar; robot bandın hemen dışında takılı kalıyorsa `MIN_TURN_FRACTION`'ı artır (1.0 = eski sert davranış), bant kenarında hâlâ sekiyorsa azalt.
3. **kP:** `Align/kP` = 0.010'dan başla. Hedefi 10-20° yandan göster, sol tetiğe bas: yumuşakça hedefe oturmalı. Salınım başlarsa değeri %30 düşür. Yavaşsa 0.012-0.015'e çık.
4. **Hizalı sayma:** `Align/Aligned` ±1.5° içinde 0.15 s kalınca true olur, ±3°'den çıkınca false.
5. **Nişan ofseti:** hizalıyken top atıp hub'ın ortasına gidip gitmediğine bak. Sürekli sağa/sola gidiyorsa `Vision/AimOffsetDeg`'i ayarla (tx bu değerde iken nişan tam ortada sayılır).

> Simülasyon (Python, kamera 6-25 fps, gecikme 0.1-0.3 s, dönüş hızı 120-450 °/s): `kP = 0.010` her durumda kararlı çıktı; eski `kP = 0.020` bazı durumlarda salındı. Gerçek robotta mutlaka tekrar dene.

---

## 10. Atış tablosu (RPM + hood) ve uçtan uca (40 dk)

**Önce hood'u devreye al** (ilk kez):
1. `Constants.CAN.HOOD_ID` = hood NEO'sunun gerçek CAN id'si (1-8 dolu; test bunu kontrol eder).
2. `HoodConstants.GEAR_RATIO` = hood dişlisi gerçekte kaç:1 (şu an 50 TAHMİN). Hood açısı HOME'dan itibaren **derece**dir.
3. `MIN_DEGREES` / `MAX_DEGREES` = hood'un mekanik olarak gidebildiği uçlar (şu an 0 / 40 TAHMİN). Önce tuning açıkken `Hood/ManualDegrees` ile **küçük adımlarla** dene.
4. Yön: `Hood/ManualDegrees`'i artırınca top daha **yükseğe** çıkmalı. Tersiyse `HoodConstants.INVERTED = true`.
5. **Robot açılırken hood HOME'da (en alçakta) olmalı.** Enkoder açılışta 0 kabul edilir (intake pivot ile aynı mantık).
6. Hood titriyorsa `Hood/kP`'yi düşür, geç kalıyorsa artır. `Hood/Ready` hedefte true olmalı.

**Tabloyu doldur:** `VisionConstants.SHOT_BY_TY_TABLE` satırları `{ty, rpm, hood derecesi}`; değerler **TAHMİNİ placeholder**. `USE_DISTANCE_SHOT = false` iken kullanılmaz (rpm ve hood manuel değerde kalır).
1. Robotu hub'a hizala, mesafeyi **şeritle** ölç (1.5 / 2.5 / 3.5 / 4.5 m gibi 4-5 nokta). Gerçek atış pozisyonlarını seç.
2. Her noktada: `Vision/Ty`'yi **not et**, sağ tetikle manuel atış yaparak topların girdiği RPM'i (`Shooter/TargetRPM`) ve hood açısını (`Hood/ManualDegrees`) bul. Her mesafede birden fazla (rpm, açı) çifti işe yarar; **tutarlı** olanı seç (örn. hep en düz yörünge ya da hep en yüksek giriş açısı).
3. `{ty, rpm, hood}` üçlülerini tabloya yaz. Yakın = büyük `ty`. RPM'ler 2500-5000, hood açıları `MIN..MAX_DEGREES` arasında olmalı (birim test kontrol eder).
4. Tabloyu doldurunca `USE_DISTANCE_SHOT = true` yap, yeniden deploy et.
5. **Uçtan uca:** sol tetik basılı: robot dönüp hizalanmalı, hood ve shooter `Vision/TableRpm` / `Vision/TableHoodDegrees` hedeflerine gitmeli, `Shoot/State` sırasıyla `SPINNING_UP → WAITING_AIM → SHOOTING`. Besleme ancak **shooter hazır + hood hazır + hizalı** olunca başlar. Tag kaybolursa besleme **durur**. Tetik bırakılınca hood HOME'a iner (B ile önceden açıldıysa manuel açıya döner).

---

## Yarışma öncesi son kontrol
- [ ] `TUNING_ENABLED = false` (dashboard'dan yanlış değer girilemesin).
- [ ] Bu gecenin değerleri `Constants.java`'ya yazıldı (kV, kP, MIN_TURN, kamera, tablo, anti-jam eşikleri, AIM_OFFSET).
- [ ] `./gradlew build` geçiyor (32 test).
- [ ] Limelight'ta iki pipeline ve `ID Filters` dolu, kamera statik IP'de, ayarlar yedeklendi.
- [ ] Robot açılırken intake kapalı **ve hood HOME'da**.

---

## Kodla ilgili bilinen belirsizlikler (donanımda doğrulanması gerekenler)
| Konu | Durum |
|---|---|
| NEO enkoderinin `inverted(true)` ile işaret değiştirmesi | Topluluk kaynağı doğruluyor, REV resmi dokümanı net değil. Emniyet: `Shooter/Fault` |
| Shooter `kP` birimi (REVLib 2026) | Doküman belirtmiyor, küçük başlayıp artır |
| LL2'nin AprilTag fps/menzili | LL2 yavaş, test 8 menzil kontrolü |
| Pipeline geçişinin gecikmesi | Kod 0.25 s aralıkla tekrar dener, `PipelineOk` olmadan hedef kullanılmaz |
| Anti-jam eşikleri (akım/rpm) | Gerçek top yüküyle ayarlanacak, test 5 |

## Kaynaklar
- Limelight 2/2+ Quick-Start: https://docs.limelightvision.io/docs/docs-limelight/getting-started/limelight-2
- Limelight indirmeler (LL2 → `limelight2_2026_0.zip`): https://docs.limelightvision.io/docs/resources/downloads
- Limelight donanım karşılaştırması: https://docs.limelightvision.io/docs/docs-limelight/getting-started/hardware-comparison
- Limelight AprilTag takibi: https://docs.limelightvision.io/docs/docs-limelight/pipeline-apriltag/apriltags
- REV Feed Forward (kV birimi: Volt/RPM): https://docs.revrobotics.com/revlib/spark/closed-loop/feed-forward-control
- 2026 AprilTag yerleşimi: WPILib `2026-rebuilt-welded.json` (`apriltag-java` jar'ı içinde)
