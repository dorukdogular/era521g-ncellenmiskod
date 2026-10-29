package frc.robot;

public final class Constants {
  private Constants() {}

  // sadece tuning gunlerinde true yap, yarismada false kalsin (dashboard'dan yanlis deger girilmesin)
  public static final boolean TUNING_ENABLED = false;

  public static final class CAN {
    private CAN() {}

    public static final int LEFT_FRONT_ID  = 1;
    public static final int LEFT_BACK_ID   = 2;
    public static final int RIGHT_FRONT_ID = 3;
    public static final int RIGHT_BACK_ID  = 4;

    public static final int INTAKE_ROLLER_ID = 5;
    public static final int INTAKE_PIVOT_ID  = 6;
    public static final int SHOOTER_ID       = 7;
    public static final int FEEDER_ID        = 8;
    // olc: hood neo'sunun gercek can id'si. 1-8 kullanimda, bos bir id yaz
    public static final int HOOD_ID          = 9;
  }

  public static final class OIConstants {
    private OIConstants() {}

    public static final int DRIVER_CONTROLLER_PORT = 0;
    public static final double TRIGGER_THRESHOLD   = 0.5;
    public static final double DEBOUNCE_SECONDS    = 0.06;
  }

  public static final class DriveConstants {
    private DriveConstants() {}

    public static final double DEADBAND        = 0.08;
    public static final double SLEW_RATE_LIMIT = 3.0;
    public static final double MAX_OUTPUT      = 1.0;

    public static final boolean LEFT_INVERTED  = false;
    public static final boolean RIGHT_INVERTED = true;

    // Cubuk isaretleri DriveCommand icinde cevrilir.
    // Xbox Y: geri pozitif, arcade ileri pozitif ister.
    // WPILib 2026 arcade donusu saat yonunun tersine pozitiftir; sag cubuk X de cevrilir.

    // cim icin 40a (eskiden 50). mini cim'e gecersen 30a yap.
    // not: revlib dokumani smart limit'in brushed'da etkisiz olabilecegini soyluyor; robot eskiden de bu sekilde calisiyordu
    public static final int CURRENT_LIMIT_BRUSHED = 40;
    public static final double OPEN_LOOP_RAMP     = 0.30;
    public static final double CLOSED_LOOP_RAMP   = 0.30;

    public static final double SAFETY_EXPIRATION = 0.20;
  }

  public static final class IntakePivotConstants {
    private IntakePivotConstants() {}

    public static final double GEAR_RATIO                  = 40.0; // olc: pivot dislisi gercekten 40:1 mi kontrol et
    public static final double POSITION_CONVERSION_FACTOR  = 360.0 / GEAR_RATIO;
    public static final double VELOCITY_CONVERSION_FACTOR  = POSITION_CONVERSION_FACTOR / 60.0;

    public static final double DEFAULT_OPEN_DEGREES   = 90.0;
    public static final double DEFAULT_CLOSED_DEGREES = 0.0;

    public static final double DEFAULT_KP = 0.020;
    public static final double DEFAULT_KI = 0.0;
    public static final double DEFAULT_KD = 0.001;
    public static final double DEFAULT_KFF = 0.0;

    // robotu her zaman intake KAPALI iken ac! enkoder acilista 0 kabul ediliyor
    public static final double SOFT_LIMIT_FORWARD = 95.0;
    public static final double SOFT_LIMIT_REVERSE = -3.0;

    public static final double OUTPUT_MIN = -0.8;
    public static final double OUTPUT_MAX =  0.8;

    public static final double POSITION_TOLERANCE_DEGREES = 2.5;

    // dashboard'dan girilen degerlerin sinirlari (tuning acikken bile gecerli)
    public static final double MAX_KP = 0.10;
    public static final double MAX_KD = 0.05;

    public static final int CURRENT_LIMIT = 30;
    public static final double RAMP_RATE  = 0.25;
  }

  public static final class HoodConstants {
    private HoodConstants() {}

    // neo pinyon 10 dis, hood dislisi 21 dis -> motor/hood = 21/10
    public static final double GEAR_RATIO                 = 21.0 / 10.0;
    public static final double POSITION_CONVERSION_FACTOR = 360.0 / GEAR_RATIO; // hood derecesi

    // hood acisi = HOME'dan (robot acilirken durdugu en alcak konum) itibaren derece. 0 = home.
    // robotu her zaman hood HOME'da iken ac! enkoder acilista 0 kabul ediliyor (intake pivot ile ayni)
    public static final double HOME_DEGREES    = 0.0;
    // olc: hood'un mekanik olarak gidebildigi en uc. soft limitler bunun biraz disinda
    public static final double MIN_DEGREES     = 0.0;
    public static final double MAX_DEGREES     = 40.0;
    public static final double SOFT_LIMIT_MARGIN = 2.0;

    // manuel atista (sag tetik) kullanilan varsayilan hood acisi, dashboard'dan degisir (tuning acikken)
    public static final double DEFAULT_DEGREES = 20.0;

    // hood donus yonu ters ise true yap (hood acisi arttikca top daha yuksege cikmali)
    public static final boolean INVERTED = false;

    public static final double DEFAULT_KP = 0.03;
    public static final double MAX_KP     = 0.10;
    public static final double OUTPUT_MIN = -0.5;
    public static final double OUTPUT_MAX =  0.5;

    // hazir: |aci - hedef| < ready_tol 0.10 sn boyunca. hazirdan cikis: > drop_tol
    public static final double READY_TOLERANCE_DEGREES = 1.0;
    public static final double DROP_TOLERANCE_DEGREES  = 2.5;
    public static final double READY_DEBOUNCE_SECONDS  = 0.10;

    // yeni hedef mevcut hedeften bu kadar azsa yok sayilir (ty gurultusu hood'u titretmesin)
    public static final double RETARGET_DEADBAND_DEGREES = 0.5;

    public static final int CURRENT_LIMIT = 30;
    public static final double RAMP_RATE  = 0.15;
  }

  public static final class IntakeRollerConstants {
    private IntakeRollerConstants() {}

    public static final double DEFAULT_SPEED = -1.0;
    // neo icin 40a guvenli. pdh kanalindaki breaker 40a degilse 30 yap
    public static final int CURRENT_LIMIT    = 40;
    public static final double RAMP_RATE     = 0.10;
  }

  public static final class ShooterConstants {
    private ShooterConstants() {}

    // neo 1.1 serbest hiz 5676 rpm. motor degistirirsen burayi da degistir
    public static final double NEO_FREE_SPEED_RPM = 5676.0;
    public static final double NOMINAL_VOLTAGE    = 12.0;

    // manuel atista (sag tetik) kullanilan varsayilan rpm, dashboard'dan degisir (tuning acikken)
    public static final double DEFAULT_TARGET_RPM = 5000.0;

    // hedef rpm'in izin verilen araligi. max'i neo'nun %88'inden fazla yapma
    public static final double MIN_TARGET_RPM = 2500.0;
    public static final double MAX_TARGET_RPM = 5000.0;

    // hiz pid: once sadece kv ile dene (kp=0), sonra kp'yi kucuk adimlarla artir
    public static final double KV = NOMINAL_VOLTAGE / NEO_FREE_SPEED_RPM; // volt / rpm
    // kv: yuksuz serbest hizdan hesaplandi. gercek flywheel yuk altinda biraz daha yavas doner, yani gercek kv BUYUKTUR.
    // dashboard'da Shooter/ImpliedKV degerini stabil rpm'de oku ve buraya yaz. kv eksik kalirsa rpm hedefin altinda takilir, hazir olmaz!
    public static final double MIN_KV_FACTOR = 0.7; // dashboard kv araligi: KV * 0.7 ... KV * 1.5
    public static final double MAX_KV_FACTOR = 1.5;
    public static final double DEFAULT_KP = 0.0002; // baslangic: topluluk ornegi kv=1/473 ile kp=0.0002 (donanimda dogrulanmadi, tune et)
    public static final double MAX_KP     = 0.002;  // dashboard bundan fazlasini kabul etmez

    // hazir: |rpm - hedef| < ready_tol 0.12 sn boyunca. hazirdan cikis: |rpm - hedef| > drop_tol
    public static final double READY_TOLERANCE_RPM = 150.0;
    public static final double DROP_TOLERANCE_RPM  = 350.0;
    public static final double READY_DEBOUNCE_SECONDS = 0.12;

    // ty gurultusu setpoint'i titretmesin. bundan kucuk rpm degisiklikleri yok sayilir
    public static final double RETARGET_DEADBAND_RPM = 50.0;

    // rpm olcum filtresi: varsayilan 32 ms / 8. daha az gecikme icin 16 ms / 4. rpm cok oynak gorunurse 32 / 8'e don
    public static final int VELOCITY_MEASUREMENT_PERIOD_MS = 16; // izinli aralik 8-64
    public static final int VELOCITY_AVERAGE_DEPTH         = 4;  // izinli: 1, 2, 4, 8

    // guvenlik: rpm bu degerin altinda NEGATIF okunursa (enkoder ters) shooter durdurulur ve fault verilir
    public static final double WRONG_DIRECTION_RPM     = 300.0;
    public static final double WRONG_DIRECTION_SECONDS = 0.30;

    // 40a: pdh kanali 40a breaker ise bunu artirma
    public static final int CURRENT_LIMIT = 40;
    public static final double OPEN_LOOP_RAMP   = 0.30;
    public static final double CLOSED_LOOP_RAMP = 0.20;
  }

  public static final class FeederConstants {
    private FeederConstants() {}

    public static final double DEFAULT_SPEED = -1.0;
    // neo icin 40a guvenli. pdh kanalindaki breaker 40a degilse 30 yap
    public static final int CURRENT_LIMIT    = 40;
    public static final double RAMP_RATE     = 0.10;
  }

  public static final class AntiJamConstants {
    private AntiJamConstants() {}

    // sikisma = komut > min_command, akim > jam_current, hiz < jam_max_rpm, jam_detect_seconds boyunca
    public static final double MIN_COMMAND         = 0.5;
    public static final double JAM_CURRENT_AMPS    = 33.0; // akim limitinin ~%80'i. limiti degistirirsen bunu da degistir
    public static final double JAM_MAX_RPM         = 250.0;
    public static final double JAM_DETECT_SECONDS  = 0.20;

    // komuttan sonraki ilk bu sure icinde algilama yok (kalkis akimi yanlis alarm vermesin)
    public static final double STARTUP_GRACE_SECONDS = 0.35;

    // sikisma tepkisi: ters yonde bu guc ve sure
    public static final double REVERSE_POWER   = 0.6;
    public static final double REVERSE_SECONDS = 0.20;

    // 3 sn icinde bu kadar sikisma olursa motoru kisa sure durdur (isinmasin)
    public static final int MAX_JAMS_IN_WINDOW     = 3;
    public static final double JAM_WINDOW_SECONDS  = 3.0;
    public static final double COOLDOWN_SECONDS    = 0.5;
  }

  public static final class ShootCommandConstants {
    private ShootCommandConstants() {}

    public static final double OSCILLATION_MAX_PERIOD = 0.45;
    public static final double ROLLER_ASSIST_SPEED    = -1.0;
  }

  public static final class VisionConstants {
    private VisionConstants() {}

    // limelight arayuzundeki hostname ile ayni olmali (varsayilan "limelight")
    public static final String LIMELIGHT_NAME = "limelight";

    // SADECE BILGI ICIN (dashboard'da Vision/Distance gosterir). rpm bunlara bagli DEGIL, rpm dogrudan ty acisindan hesaplanir.
    // kamera egiminde 1 derece hata 3 m'de ~0.26 m mesafe hatasi yapar, bu yuzden mesafeyi rpm icin kullanmiyoruz.
    // olc: lens merkezinin yerden yuksekligi, metre. simdiki deger TAHMINI
    public static final double CAMERA_HEIGHT_METERS = 0.50;
    // olc: kameranin yataya gore yukari acisi, derece (yukari pozitif). simdiki deger TAHMINI
    public static final double CAMERA_PITCH_DEGREES = 25.0;
    // 2026 hub tag merkezi yuksekligi (wpilib 2026-rebuilt layout). degistirme
    public static final double TAG_HEIGHT_METERS = 1.124;

    // hub tag'leri (wpilib 2026-rebuilt-welded.json). hub'in her yuzunde 2 tag var: 1 tanesi yuzun TAM ORTASINDA,
    // digeri 0.355 m yanda. yandaki tag'e nisan alirsan 3 m'de ~6.8 derece sapar. bu yuzden SADECE ortadaki tag'ler:
    // kirmizi hub: 2, 4, 5, 10   |   mavi hub: 18, 20, 21, 26
    public static final int[] RED_HUB_TAG_IDS  = {2, 4, 5, 10};
    public static final int[] BLUE_HUB_TAG_IDS = {18, 20, 21, 26};

    // limelight arayuzunde 2 AprilTag pipeline olustur ve her birinin "ID Filters" kutusuna yukaridaki id'leri yaz:
    // pipeline 0 -> "2,4,5,10" (kirmizi), pipeline 1 -> "18,20,21,26" (mavi). kod alliance'a gore pipeline'i secer.
    // (fiducial_id_filters_set sadece localization'i filtreler, tx/ty'yi degil, o yuzden pipeline kullaniyoruz)
    public static final int RED_PIPELINE_INDEX  = 0;
    public static final int BLUE_PIPELINE_INDEX = 1;

    // nisan ofseti: shooter topu hub'a atarken limelight'in gosterdigi tx bu degerdeyse tam ortalidir.
    // kamera shooter ekseninin tam ustunde degilse bunu ayarla. tx pozitif = tag kameranin sagi
    public static final double AIM_OFFSET_DEGREES = 0.0;

    // limelight verisi (heartbeat) bu kadar sure guncellenmezse hedef yok sayilir
    public static final double STALE_DATA_SECONDS = 0.5;

    // bilgi amacli mesafe hesabinin gecerli araligi
    public static final double MIN_VALID_DISTANCE_METERS = 0.8;
    public static final double MAX_VALID_DISTANCE_METERS = 6.0;

    // true: gecerli ty varken rpm ve hood tablodan gelir, PID oturunca besleme baslar.
    // false: eski davranis, hep manuel rpm/hood.
    // TABLO HALA OLCULMEMIS yer tutucu. Egri dogru yone gider ama sayilar atis gunu degismeli.
    public static final boolean USE_DISTANCE_SHOT = true;

    // tek karede ty'nin tabloya yansiyabilecegi en fazla degisim (20 ms dongu basina derece).
    // ani tag sicramasi rpm'i ziplatmasin; gercek surus bundan yavas ty degistirir.
    public static final double TY_SLEW_DEGREES_PER_CYCLE = 0.5;

    // {ty (derece), rpm, hood acisi (derece)}. ty = limelight'in gosterdigi dikey aci (dashboard: Vision/Ty).
    // hedefe YAKINKEN ty BUYUK, uzakken kucuk (negatif).
    // doldurma: robotu hub'a belli mesafelerde (4-5 nokta, seritle olc) hizala, Vision/Ty'yi oku,
    // top girene kadar rpm'i ve hood acisini ayarla (Shooter/TargetRPM, Hood/ManualDegrees), ucunu de yaz.
    // satir sirasi onemli degil ama ayni ty iki kez olmasin. rpm'ler MIN/MAX_TARGET_RPM, hood acilari
    // HoodConstants.MIN/MAX_DEGREES araliginda olmali.
    // asagidakiler TAHMINI placeholder. mutlaka olc ve degistir.
    // uc satirlar, rehberdeki yakin (~+11 ty) ve uzak ucun tablo disina dusmemesi icin
    // ayni egimle uzatildi. 5000 rpm tavani yuku tutmayabilir; olcumde dusur.
    public static final double[][] SHOT_BY_TY_TABLE = {
        {-12.0, 5000.0, 35.0},
        { -8.0, 5000.0, 30.0},
        { -4.0, 4400.0, 25.0},
        {  0.0, 3800.0, 20.0},
        {  6.0, 3200.0, 12.0},
        { 12.0, 2600.0,  4.0},
    };

    // tablonun ty araliginin disina bu kadar derece tasarsa rpm hesaplanmaz (guvenli varsayilana doner)
    public static final double TABLE_MARGIN_DEGREES = 1.5;
  }

  public static final class AlignConstants {
    private AlignConstants() {}

    // hizalama pid: tx (derece) -> donus gucu. once kp ile dene, salinim olursa kp'yi dusur
    // limelight 2 apriltag'de yavas (fps dusuk + gecikme ~100-250 ms). yuksek kp salinim yapar, dusuk basla.
    // kp'yi salinim baslayana kadar artir, sonra %40 geri cek. kd'yi 0 birak (yavas veride zararli)
    public static final double KP = 0.010;
    public static final double KI = 0.0;
    public static final double KD = 0.0;

    public static final double TOLERANCE_DEGREES      = 1.5; // hizali sayilma esigi
    public static final double EXIT_TOLERANCE_DEGREES = 3.0; // hizadan cikma esigi (histerezis)
    public static final double ALIGNED_DEBOUNCE_SECONDS = 0.15;

    public static final double MAX_TURN_OUTPUT = 0.35; // donus guc tavani (dusuk fps'te hizli donmek asiri gecmeye yol acar)
    // olc: robotu yerde yavasca artan guc ile dondur (tuning acikken Align/MinTurn), dondugu ilk gucu bul, ustune +0.03 ekle.
    // cok dusuk olursa robot hedefin yakininda takili kalir, cok yuksek olursa saga sola titrer (simulasyonla dogrulandi)
    public static final double MIN_TURN_OUTPUT = 0.10;
    public static final double MAX_KP = 0.05;          // dashboard'dan girilebilecek ust sinir

    // min donus gucu hedefe yaklasirken yumusar: |tx| >= FADE_DEGREES iken tam MIN_TURN_OUTPUT,
    // tolerans kenarinda MIN_TURN_FRACTION * MIN_TURN_OUTPUT. boylece bant kenarinda 0.10 -> 0 sicramasi azalir.
    // robot bantin yakininda takili kaliyorsa MIN_TURN_FRACTION'i artir (1.0 = eski davranis)
    public static final double FADE_DEGREES      = 6.0;
    public static final double MIN_TURN_FRACTION = 0.7;

    // WPILib 2026 arcade donusu saat yonunun tersine pozitiftir.
    // TURN_SIGN +1 iken tx>0 (hedef sagda) negatif, yani saat yonunde donus uretir.
    // bench'te ters donuyorsa -1.0 yap.
    public static final double TURN_SIGN = 1.0;
  }
}
