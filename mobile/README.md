# MELARIUM Mobile

Android va iOS uchun Flutter ilovasi. Ilova backend REST API bilan ishlaydi: OTP orqali kirish, sessiyani yangilash va xavfsiz tugatish, shuningdek server qaytargan rolga mos sahifalar. Yangi hisobning roli `USER`; boshqa rollarni vakolatli administrator tayinlaydi.

## Talablar

- Flutter SDK 3.47.9 va mos Dart SDK (3.13.7)
- Android build uchun Android SDK va JDK 21
- iOS build uchun macOS va Xcode

## API manzili

`API_BASE_URL`-ni `--dart-define` orqali o‘zgartirish mumkin. Qiymat `/api/v1` bilan tugasin.

- Android emulator: `http://10.0.2.2:8080/api/v1` (standart qiymat; faqat debug HTTP sozlamasi bilan).
- Fizik Android qurilma: kompyuterning LAN manzili va porti, masalan `https://api.example.uz/api/v1`.
- iOS Simulator va release build: HTTPS manzilidan foydalaning. iOS App Transport Security oddiy HTTP so‘rovlarini bloklashi mumkin; mahalliy HTTP sinovi uchun faqat debug konfiguratsiyasida xavfsizlik istisnosini sozlab, release’dan oldin olib tashlang.

Production uchun HTTPS shart. Android’dagi ochiq HTTP ruxsati faqat debug manifestida; release’da ochiq matnli trafik yoqilmagan.

## Ishga tushirish va tekshiruv

```bash
cd mobile
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

Android APK yig‘ish:

```bash
# Xotira cheklangan muhitda arm64 debug APK
flutter build apk --debug --target-platform=android-arm64 \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# Release uchun production HTTPS manzili va loyihaning signing sozlamalari
flutter build apk --release \
  --dart-define=API_BASE_URL=https://api.example.uz/api/v1
```

Tekshirilgan yuklab olinadigan APK — `arm64-v8a` uchun debug build; bu Play Store release imzosi emas. Universal/multi-ABI build ushbu xotira cheklangan Linux muhitida tekshirilmagan.

iOS yig‘ish (macOS/Xcode’da):

```bash
flutter build ios --release \
  --dart-define=API_BASE_URL=https://api.example.uz/api/v1
```

OTP/SMS yetkazib berish, real to‘lovlar va production API manzili server integratsiyasi hamda tegishli credentials sozlanishiga bog‘liq; ilova demo yoki development sozlamalari production xizmatlari mavjudligini anglatmaydi.
