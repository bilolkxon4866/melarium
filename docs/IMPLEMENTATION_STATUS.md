# MELARIUM — loyiha holati

Oxirgi yangilanish: **2026-11-04**

## Amalga oshirilgan

- [x] Monorepo: `frontend/`, `backend/`, `mobile/`, `docs/`.
- [x] React + TypeScript web ilovasi: public marketplace, investor, asalarichi, oddiy foydalanuvchi va admin sahifalari. CSS’da desktop, tablet va telefon ekranlariga mos responsive breakpointlar mavjud.
- [x] Spring Boot REST API: OTP kirish, JWT access/refresh, refresh token rotation va logout, server-side RBAC, audit va PostgreSQL/Flyway.
- [x] Flyway V1–V6 asosiy schema va oldingi funksiyalar uchun mavjud.
- [x] Auth kodi email+telefonni so‘raydi, telefon OTP’siga kiritilgan emailni bog‘laydi, emailni hisobda saqlaydi va imtiyozli rollarni serverdagi to‘liq email+telefon juftligi bilan belgilaydi. Super Admin `ADMIN` huquqini ham oladi.
- [x] V7 email uniqueness/auth migratsiyasi PostgreSQL 17.11’da synthetic duplicate email’lar, email_hash OTP ustuni va email bilan bog‘lanmagan eski OTP’larni bekor qilish bilan smoke-test qilindi; Hibernate `validate` orqali sxema mosligi tasdiqlandi. Production ma’lumotlar nusxasida alohida tekshiruv hali zarur. Eski phone-only bootstrap role’lari birinchi muvaffaqiyatli OTP loginida juftlikka ko‘ra qayta tekshiriladi.
- [x] Apiary/colony CRUD, listing moderation, harvest review, investor/beekeeper/admin reports, sensor readings, support ticket create/review va tegishli UI/API.
- [x] Flutter Android/iOS mobil ilovasining manba kodi: API client, OTP/session, secure token storage va role-based ekranlar.
- [x] Uzbek Latin UI matnlari tekshirildi; tekshirilgan source va README fayllarida kirill belgisi topilmadi.
- [x] Android arm64 debug APK oldingi source’dan build qilingan; email+telefon auth o‘zgarishlari APK’ga kiritilmagan. Ilova nomi `MELARIUM`.
- [x] Git remote: `bilolkxon4866/melarium`, asosiy branch: `main`. `main` ga push production deploy workflow’ni ishga tushirishi mumkin; live deployment natijasi va backend secret’lari alohida tekshirilishi lozim.
- [x] Hozirgi deploy qaydi: Vercel frontend, backend esa GitHub Actions orqali serverga yuboriladi (`docs/DEPLOY.md`). Vercel Container Runtime’ga backend ko‘chirilmagan; `docs/RAILWAY_DEPLOY.md` tarixiy qo‘llanma.

## Tekshiruv natijalari

- Joriy web tekshiruvlari: `npm run typecheck` va `npm run build` muvaffaqiyatli.
- Backend: `mvn -q -f backend/pom.xml test` — 9 test muvaffaqiyatli.
- PostgreSQL 17.11’da Flyway V1–V7 migratsiyalari toza database va synthetic duplicate email ma’lumotlari bilan smoke-test qilindi, Hibernate `validate` startup’da tekshirildi. Production ma’lumotlar nusxasi ishlatilmadi; Flyway PostgreSQL 17 bo‘yicha moslik warning berdi.
- Flutter/Dart SDK bu muhitda yo‘q, shuning uchun yangi mobile auth uchun `flutter analyze`/`flutter test` bajarilmadi.
- Oldingi API smoke-test: health `200`; yangi foydalanuvchi `USER`; refresh token rotation va logout tekshirildi; oddiy foydalanuvchining admin status so‘rovi `403`; bootstrap `SUPER_ADMIN` so‘rovi `200`; support murojaati yaratilib, admin tomonidan `IN_REVIEW` holatiga o‘tkazildi. Bu natija yangi email+telefon OTP oqimining live end-to-end tekshiruvini anglatmaydi.
- Oldingi Android APK: `arm64-v8a`, debug build, min SDK 24; email+telefon auth o‘zgarishlari kiritilmagan. API URL Android emulator uchun `http://10.0.2.2:8080/api/v1`.
- Oldingi Railway image build natijasi tarixiy; joriy deploy arxitekturasi `docs/DEPLOY.md` da qayd etilgan va Vercel frontend/VM backend’dan iborat.

## Hali tasdiqlanmagan yoki egasi sozlashi kerak bo‘lganlar

- [ ] iOS build va iPhone/iOS Simulator smoke-test: macOS va Xcode kerak; bu Linux muhitida build qilib bo‘lmaydi.
- [ ] Universal/multi-ABI Android APK: xotira cheklangan Linux muhitida tasdiqlanmagan. Hozirgi APK faqat arm64 debug build.
- [ ] Release APK signing, production HTTPS API domeni va haqiqiy qurilmada API ulanishini sozlash.
- [ ] SMS, push, payment gateway/webhook va IoT qurilmalarining haqiqiy integratsiyasi: provider credentials va shartnomalar berilmagani uchun haqiqiy oqimlar yoqilmagan.
- [ ] Investitsiyaning huquqiy modeli, ROI/daromad, xavf, refund va KYC shartlarini loyiha egasi tasdiqlashi kerak. Tasdiqlanmaguncha daromad kafolatlanmaydi va real pul oqimi yoqilmaydi.
- [ ] Production backend environment’iga Admin va Super Admin email+telefon juftliklari, DB/SMS qiymatlari kiritilishi kerak. Yangi production versiya bu juftliklarsiz ishga tushmaydi.
- [ ] Vercel Container Runtime’ga backend ko‘chirish, agar tanlansa: Docker/Container porti, API rewrite va billing alohida sozlanib, live deploy tekshirilishi kerak. Hozirgi workflow backend’ni Vercel’da ishga tushirmaydi.
- [ ] Haqiqiy browser/device UAT, monitoring va backup rejasi.


## Yetkazib berish fayllari

- Android APK: `/home/user/MELARIUM-android-debug-arm64.apk` (debug, arm64, emulator API URL).
- Source archive: `/home/user/MELARIUM-source.zip` (dependency va build cache kiritilmagan).
- Ishga tushirish ko‘rsatmalari: asosiy `README.md` va `mobile/README.md`.
