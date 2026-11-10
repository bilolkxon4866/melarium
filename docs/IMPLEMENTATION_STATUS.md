# MELARIUM — loyiha holati

Oxirgi yangilanish: **2026-11-04**

## Amalga oshirilgan

- [x] Monorepo: `frontend/`, `backend/`, `mobile/`, `docs/`.
- [x] React + TypeScript web ilovasi: public marketplace, investor, asalarichi, oddiy foydalanuvchi va admin sahifalari. CSS’da desktop, tablet va telefon ekranlariga mos responsive breakpointlar mavjud.
- [x] Spring Boot REST API: OTP kirish, JWT access/refresh, refresh token rotation va logout, server-side RBAC, audit va PostgreSQL/Flyway.
- [x] Flyway migratsiyalari V1–V5: asosiy schema, authentication, admin/orders/preferences, support tickets va `USER` roli.
- [x] Yangi hisob `USER` roli bilan ochiladi; `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE`-ga mos raqam orqali OTP bilan tasdiqlangan foydalanuvchi `SUPER_ADMIN` bo‘ladi. Admin tizim holati endpointi maxfiy qiymatlarni qaytarmaydi.
- [x] Apiary/colony CRUD, listing moderation, harvest review, investor/beekeeper/admin reports, sensor readings, support ticket create/review va tegishli UI/API.
- [x] Flutter Android/iOS mobil ilovasining manba kodi: API client, OTP/session, secure token storage va role-based ekranlar.
- [x] Uzbek Latin UI matnlari tekshirildi; tekshirilgan source va README fayllarida kirill belgisi topilmadi.
- [x] Android arm64 debug APK build qilindi. Ilova nomi `MELARIUM`.

## Tekshiruv natijalari

- `npm run typecheck` va `npm run build` — muvaffaqiyatli.
- `mvn -q -f backend/pom.xml test` — 4 test muvaffaqiyatli.
- `flutter analyze` — muammo topilmadi; `flutter test` — 2 test muvaffaqiyatli.
- PostgreSQL 17.11’da Flyway V1–V5 migratsiyalari muvaffaqiyatli; Hibernate `validate` xatosiz yakunlandi. Flyway versiyasi PostgreSQL 17 bo‘yicha moslik ogohlantirishini berdi; rasmiy tekshirilgan eng so‘nggi major versiya PostgreSQL 16.
- API smoke-test: health `200`; yangi foydalanuvchi `USER`; refresh token rotation va logout tekshirildi; oddiy foydalanuvchining admin status so‘rovi `403`; bootstrap `SUPER_ADMIN` so‘rovi `200`; support murojaati yaratilib, admin tomonidan `IN_REVIEW` holatiga o‘tkazildi.
- Android APK: `arm64-v8a`, debug build, min SDK 24. API URL Android emulator uchun `http://10.0.2.2:8080/api/v1`.

## Hali tasdiqlanmagan yoki egasi sozlashi kerak bo‘lganlar

- [ ] iOS build va iPhone/iOS Simulator smoke-test: macOS va Xcode kerak; bu Linux muhitida build qilib bo‘lmaydi.
- [ ] Universal/multi-ABI Android APK: xotira cheklangan Linux muhitida tasdiqlanmagan. Hozirgi APK faqat arm64 debug build.
- [ ] Release APK signing, production HTTPS API domeni va haqiqiy qurilmada API ulanishini sozlash.
- [ ] SMS, push, payment gateway/webhook va IoT qurilmalarining haqiqiy integratsiyasi: provider credentials va shartnomalar berilmagani uchun haqiqiy oqimlar yoqilmagan.
- [ ] Investitsiyaning huquqiy modeli, ROI/daromad, xavf, refund va KYC shartlarini loyiha egasi tasdiqlashi kerak. Tasdiqlanmaguncha daromad kafolatlanmaydi va real pul oqimi yoqilmaydi.
- [ ] Haqiqiy browser/device UAT, production deploy, monitoring va backup rejasi.
- [ ] `https://github.com/bilolkxon4866/melarium.git` public repositorysi mavjud va bo‘sh; push autentifikatsiya sozlangach bajariladi. Loyiha hozircha GitHub’ga yuborilmagan.

## Yetkazib berish fayllari

- Android APK: `/home/user/MELARIUM-android-debug-arm64.apk` (debug, arm64, emulator API URL).
- Source archive: `/home/user/MELARIUM-source.zip` (dependency va build cache kiritilmagan).
- Ishga tushirish ko‘rsatmalari: asosiy `README.md` va `mobile/README.md`.
