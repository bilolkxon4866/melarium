# MELARIUM

MELARIUM — asalarichilik xo‘jaliklari, investorlar va administratorlar uchun web va mobil platforma. Ushbu monorepo MELARIUM texnik topshirig‘i asosida MVP sifatida ishlab chiqilmoqda.

> **Holat:** auth endi email+telefonni so‘raydi, telefon OTP’sini saqlab qoladi va rollarni serverdagi juftliklardan belgilaydi. Joriy tekshiruvda web `npm run typecheck`/`npm run build` va backend `mvn -f backend/pom.xml test` (9 ta test) muvaffaqiyatli. Flyway V1–V7 migratsiyasi PostgreSQL 17.11’da sun’iy duplicate email ma’lumotlari bilan smoke-test qilindi; Hibernate `validate` ham startup’da o‘tdi. Flyway PostgreSQL 17.11 uchun moslik ogohlantirishi chiqardi; production ma’lumotlar nusxasida alohida tekshiruv talab etiladi. Flutter/Dart SDK bu muhitda yo‘q, shuning uchun yangi mobile auth o‘zgarishi `flutter analyze`/`flutter test` bilan tekshirilmagan; Android arm64 APK oldingi source’dan tuzilgan. iOS build Linux’da tekshirilmagan; buning uchun macOS/Xcode kerak. Bu MVP ishlab chiqarish muhiti uchun tayyor platforma deb hisoblanmaydi. SMS, push, to‘lov provayderi va haqiqiy IoT sensor integratsiyalari credentials hamda loyiha egasining tasdig‘ini kutmoqda.

## Tuzilma

- `frontend/` — React, TypeScript, Vite; public listing/FAQ, investor, asalarichi va admin kabinetlari.
- `backend/` — Java 17+, Spring Boot 3, REST/JSON, PostgreSQL, Flyway, JWT access/refresh, OTP, role-based access, audit, OpenAPI.
- `mobile/` — Flutter Android/iOS client source.
- `docs/` — loyiha, arxitektura va API bo‘yicha hujjatlar.
- `docker-compose.yml` — lokal PostgreSQL.

## Lokal ishga tushirish

Talab: Node.js 20+, JDK 17+, Maven 3.9+, Docker Compose.

### 1) PostgreSQL va backend

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

Backend odatda `http://localhost:8080` da ishga tushadi. Flyway schema migratsiyalarini qo‘llaydi; Hibernate mavjud schema’ni `validate` qiladi.

- Health: `GET /api/v1/health`
- OpenAPI: `/api-docs`
- Swagger UI: `/swagger-ui.html`

Lokal profil OTP’ni development usulida chiqarishi mumkin. Bu rejim faqat lokal sinov uchun: production’da `AUTH_EXPOSE_OTP=false` bo‘lishi, `SMS_PROVIDER` esa tasdiqlangan SMS gateway va credential’lar bilan sozlanishi kerak.

### 2) Admin va Super Admin roli juftliklari

Ro‘yxatdan o‘tish va kirishda email hamda O‘zbekiston telefon raqami kiritiladi; tasdiqlash OTP kodi hanuz SMS orqali faqat telefonga yuboriladi. Backend emailni OTP so‘roviga bog‘laydi, tekshirilgan raqamdagi hisobga muvaffaqiyatli OTP tasdiqlangach emailni saqlaydi. Email alohida OTP bilan tasdiqlanmaydi.

Imtiyozli rol mijoz tanlovidan emas, backend muhitidagi **email + telefonning ikkalasi ham** aniq mos kelishidan kelib chiqadi:

- Boshqa yangi kombinatsiya — `USER`.
- `APP_SECURITY_ADMIN_EMAIL` + `APP_SECURITY_ADMIN_PHONE` — `ADMIN`.
- `APP_SECURITY_SUPER_ADMIN_EMAIL` + `APP_SECURITY_SUPER_ADMIN_PHONE` — `ADMIN` va `SUPER_ADMIN`.

Juftlikning faqat bitta qiymatini berish backend’ni ishga tushirishda xato beradi. Lokal testda qiymatlarni shell’da bering:

```bash
export APP_SECURITY_ADMIN_EMAIL='admin@example.com'
export APP_SECURITY_ADMIN_PHONE='+998901234567'
export APP_SECURITY_SUPER_ADMIN_EMAIL='superadmin@example.com'
export APP_SECURITY_SUPER_ADMIN_PHONE='+998907654321'
```

Production’da qiymatlarni Spring Boot backend ishlayotgan muhitning **server-side environment/secrets** sozlamasiga kiriting. Joriy deploy’da backend serverda ishlaydi; faqat Vercel frontend loyihasiga qo‘shilgan qiymatlar backend’ga avtomatik o‘tmaydi. Backend’ni Vercel Container Runtime’ga ko‘chirgan taqdirdagina Vercel Project → Settings → Environment Variables’da Production/Preview’ni tanlab kiriting va redeploy qiling. `VITE_` prefiksini qo‘ymang. Email va telefonlarni chatga yoki Git’ga yubormang. Eski `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE` phone-only bootstrap endi ishlatilmaydi.

Mavjud emaili bo‘lmagan eski foydalanuvchilar telefon OTP’sini qayta tasdiqlagach kiritgan emaili hisobga biriktiriladi. V7 migration email’larni kichik harfga normallashtiradi va case-insensitive uniqueness o‘rnatadi; avvaldan takrorlangan emaillarda eng eski hisobda qiymat qoladi, qolganlari `NULL` qilinadi. Eski phone-only bootstrap’dan kelgan system `ADMIN`/`SUPER_ADMIN` grantlari mavjud DB’da saqlandi, lekin yangi backend’dagi ilk muvaffaqiyatli OTP loginida email+telefon juftligiga ko‘ra qayta tekshiriladi. **Production deploydan oldin yangi Admin va Super Admin juftliklarini backend environment’ga sozlang**; to‘liq pair’lar bo‘lmasa production backend ataylab ishga tushmaydi. Investor yoki asalarichi rollarini vakolatli administrator tayinlaydi.

### 3) Web

Root katalogida:

```bash
npm ci
npm run dev
```

Vite odatda `http://localhost:5173` da ochiladi va `/api` so‘rovlarini `http://localhost:8080` ga proxy qiladi. Tekshiruv va production build:

```bash
npm run typecheck
npm run build
```

Public qismda API’dan olingan listinglar, FAQ va hisobga kirgandan keyin murojaat yuborish yordam sahifasi bor. Investor buyurtmasi odatda vaqtinchalik rezerv hisoblanadi, to‘lov emas.

### 4) Flutter

Talab: Flutter stable SDK; Android build/run uchun Android SDK; iOS build uchun macOS va Xcode.

```bash
cd mobile
flutter pub get
flutter analyze
flutter test
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

Android emulator uchun standart API URL `http://10.0.2.2:8080/api/v1`; `localhost` host kompyuter emas. Fizik qurilma yoki iOS uchun muhitga mos URL bering; production’da HTTPS ishlating. Debug APK misoli: `flutter build apk --debug --target-platform=android-arm64`. Boshqa build variantlari va API sozlamalari `mobile/README.md` da.

## Xavfsizlik va konfiguratsiya

- JWT access token qisqa muddatli, refresh token aylantiriladi va qayta ishlatilgan token bekor qilinadi.
- Server role’lari: `USER`, `INVESTOR`, `BEEKEEPER`, `ADMIN`, `SUPER_ADMIN`. Auth orqali `USER`/`ADMIN`/`SUPER_ADMIN` rollari serverdagi email+telefon juftligi bilan belgilanadi; Super Admin `ADMIN` huquqini ham oladi. Investor va asalarichi rollarini vakolatli administrator tayinlaydi.
- Development JWT secret, OTP pepper va DB paroli default qiymatlari **faqat lokal muhit** uchun. Production’da `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET_BASE64`, `OTP_PEPPER_BASE64`, `SMS_API_URL`, `SMS_API_TOKEN`, `SMS_SENDER_ID` va to‘rtta `APP_SECURITY_*_EMAIL/PHONE` qiymatini server-side secret manager/environment orqali belgilang. Qiymatlarni qanday olish va qo‘shish bo‘yicha `docs/VERCEL_DEPLOY.md` ni ko‘ring.
- Production backend ikkala to‘liq Admin va Super Admin juftligini talab qiladi. Eski `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE` endi rol bermaydi; oddiy administrator Super Admin rolini tayinlay olmaydi.
- API’da validatsiya, resurs egasi tekshiruvi va sezgir amallar uchun audit yozuvlari mavjud. Ishga tushirishdan oldin deployment, logging, backup va rate-limit siyosatlari alohida ko‘rib chiqilishi kerak.

## Mavjud asosiy oqimlar

- OTP login/ro‘yxatdan o‘tish, me/profile, JWT refresh/logout.
- Public marketplace; asalarichi apiary/colony va listing yaratish/tahrirlash; admin listing moderatsiyasi va foydalanuvchi rollari/holati.
- Investor buyurtmasini vaqtincha band qilish, buyurtma tarixini ko‘rish; payment API webhook mavjud, ammo merchant credentials yo‘qligi uchun haqiqiy to‘lov yo‘q.
- Hosil va xarajat qaydlari, hosil moderatsiyasi, CSV hisobotlari, manual sensor readings. Manual o‘lchovlar IoT telemetriyasi sifatida ko‘rsatilmaydi.
- In-app notification preferences va audit log.
- Foydalanuvchidan yordam/texnik muammo/shikoyat murojaati, admin ko‘rib chiqishi va audit.

Tekshirish:

```bash
npm run typecheck
npm run build
mvn -f backend/pom.xml test
cd mobile && flutter analyze && flutter test
```

Backend Maven testlarining 9 tasi muvaffaqiyatli. PostgreSQL 17.11’da V1–V7 migratsiyalari, takroriy emailni normallashtirish, eski emaili bog‘lanmagan OTP’larni bekor qilish va Hibernate `validate` startup smoke-testdan o‘tdi; bu production ma’lumotlar nusxasida alohida tekshiruv o‘tkazish zaruratini bekor qilmaydi. Flyway PostgreSQL 17 uchun moslik ogohlantirishi beradi, shuning uchun production PostgreSQL/Flyway versiyasini alohida tekshiring. Flutter/Dart SDK bo‘lmagani uchun yangi mobile auth analyzer/test’lari bajarilmadi. Haqiqiy SMS, payment webhook va iOS release oqimlari credentials hamda tegishli build muhitida alohida tasdiqlanishi kerak.

## Joylashtirish

Joriy avtomatik joylashtirish oqimi `docs/DEPLOY.md` da qayd etilgan: frontend Vercel’da, backend esa GitHub Actions orqali serverga yuboriladi. `docs/RAILWAY_DEPLOY.md` eski Railway qo‘llanmasi bo‘lib, joriy deploy yo‘li emas. Backend secret’lari Vercel frontend’iga avtomatik uzatilmaydi; ularni Spring Boot ishlayotgan server muhitiga kiritish bo‘yicha bosqichlar `docs/VERCEL_DEPLOY.md` da berilgan. Backend’ni Vercel Container Runtime’ga ko‘chirish alohida deploy konfiguratsiyasi va tekshiruvini talab qiladi; hozircha buni bajarilgan deb hisoblamang.

## Hali egasi tasdiqlashi shart bo‘lganlar

Quyidagi ishlarni texnik taxmin bilan yoqib bo‘lmaydi: investitsiya va ari oilasiga egalik/ulushning huquqiy modeli; ROI, daromad formulasi yoki kafolat; komissiya va daromad taqsimoti; kasallik/yo‘qotish/force majeure xavfini kim o‘z zimmasiga olishi; muddatidan oldin chiqish/refund; KYC va moderatsiya siyosati; foydalanuvchi shartnomasi, privacy/consent; Click/Payme/Uzcard/Humo merchant contract; SMS/push credentials; IoT device protokoli va secret; operator aloqa ma’lumoti; hosting/backup va production domen. Bular tasdiqlanmaguncha haqiqiy pul olish, daromadni va’da qilish yoki sensor ma’lumotini avtomatik/verified deb atash mumkin emas.
