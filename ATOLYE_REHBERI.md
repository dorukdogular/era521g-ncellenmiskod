# Atölye rehberi

Takım 10521. Atış gününde yapılacak üç iş aşağıda. Üç ışık yeşilse dişli, kV, kP ve kamera açısına dokunma.

Robotu her açışta **hood en altta**, **intake kapalı** olsun. Enkoder o anda sıfırlanır. Hood’u elle oynatıp öyle açma.

Pratikte `Constants.java` içinde `TUNING_ENABLED = true` yapıp bir kez deploy et. Bu kapalıyken dashboard’daki RPM ve hood ayarı okunmaz. Maçtan önce tekrar `false` yap.

Dashboard’da şunlara bak: `Shooter/Ready`, `Hood/Ready`, `Align/Aligned`, `Vision/HasTarget`, `Vision/Ty`, `Shoot/State`.

---

## Kumanda

| Tuş | Ne yapar |
|---|---|
| Sol çubuk | İleri / geri |
| Sağ çubuk | Dönüş. Sağa itince saat yönü |
| A | Intake roller. Yalnız pivot açıkken döner |
| B | Shooter’ı elle aç/kapa |
| D-pad | Intake pivot aç/kapa |
| Sağ tetik veya sağ bumper | Atış. Tag menzildeyse hood ve RPM mesafeden gelir, oturunca besler. Tag yoksa dashboard’daki RPM ve hood ile atar |
| Sol tetik veya sol bumper | Aynı atış, bir de hub etiketine döner. Besleme hizalanınca başlar |

Kumanda, feeder dönerken titrer. Tetiği bırakınca besleme durur. Shooter, B ile önceden açılmadıysa durur.

Atış sırası: `SPINNING_UP`, sonra sol tetikte `WAITING_AIM`, sonra `SHOOTING`.

---

## 1. Limelight

Kameranın web sayfası: `http://10.105.21.11:5801`. Hostname `limelight` kalsın. Takım numarası 10521.

İki AprilTag pipeline aç:

| Pipeline | ID Filters |
|---|---|
| 0 | `2,4,5,10` (kırmızı hub, ortadaki tag’ler) |
| 1 | `18,20,21,26` (mavi hub, ortadaki tag’ler) |

Crosshair’e dokunma, merkezde kalsın. Kamera yüksekliğini ve eğimini ölçme; atış bunlara bakmaz, `Vision/Ty` değerine bakar.

Kontrol: kırmızı ortadaki tag’te `Vision/HasTarget` true, yandaki tag’te false. Mavi için pipeline 1 aynı şekilde.

---

## 2. Yön

Tekerlekler havada:

- Çubuk ileri: tekerlek ileri gitmeli.
- Tag sağdayken sol tetik: robot saat yönünde dönmeli.

Tersse dur ve söyle. İşareti kodda ben çeviririm. Başka inversion değiştirme.

---

## 3. Atış tablosu

Dashboard’da `Shoot/UseDistance` kapat. Sağ tetik o zaman `Shooter/TargetRPM` ve `Hood/ManualDegrees` kullanır.

Yakın, orta ve uzak dur. Top girene kadar RPM ve hood açısını değiştir. Her noktada şunları yaz:

`Vision/Ty`, giren RPM, giren hood açısı.

Üç satırı gönder. Tabloyu `Constants.java` içine ben koyarım. Sonra `Shoot/UseDistance` tekrar aç.

Sol tetikle dene. `Shooter/Ready`, `Hood/Ready` ve `Align/Aligned` birlikte yeşil olunca besleme başlar. `Vision/OutOfRange` true ise o `ty` tablonun dışında, o noktayı da yaz.

---

## Işık yanmazsa

| Ne görüyorsun | Ne yap |
|---|---|
| `Shooter/Ready` false, `Shooter/Fault` true | Dur, söyle |
| `Shooter/Ready` false, RPM hedefin altında takılı | `Shooter/ImpliedKV` değerini söyle |
| `Hood/Ready` false, hood kıpırdamıyor | Hood Spark’ın CAN numarasını söyle (kodda 9) |
| `Hood/Ready` false, hood hedefe varmıyor veya ters gidiyor | Söyle. Dişli oranı ve yön tahmini |
| `Align/Aligned` false, robot hedefte sağa sola kaçıyor | Söyle |
| Top sürekli sağa veya sola gidiyor | `Vision/Tx` değerini söyle |
| Feeder veya roller topu ters itiyor | Söyle |

Bu satırlar dışında sabitle oynama.

---

## Otonom

Toplar robotun içinde olsun, hub etiketi görünsün. Robot kabaca hub’a baksın. Otonom hizalar, hood ve RPM’i tablodan ayarlar, hazır olunca atar ve süre bitene kadar devam eder. Top saymaz, yerden top toplamaz.

---

## Maçtan önce

- Ölçülen tablo `Constants.java` içinde, `Shoot/UseDistance` açık.
- `TUNING_ENABLED = false`, yeniden deploy.
- İki pipeline duruyor.
- Açılışta intake kapalı, hood en altta.
