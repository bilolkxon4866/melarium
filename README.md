# MELARIUM

MELARIUM — asalarichilik xo‘jaliklari, investorlar va administratorlar uchun web va mobil platforma. Ushbu monorepo MELARIUM texnik topshirig‘i asosida MVP sifatida ishlab chiqilmoqda.

> **Holat:** web/backend oqimlari va Flutter API clienti mavjud. `flutter analyze`, `flutter test` (2 test) hamda backend testlar (4 test) o‘tdi; Android arm64 debug APK muvaffaqiyatli yig‘ildi; universal/multi-ABI APK bu xotira cheklangan muhitda tekshirilmagan. Flyway V1–V5 migratsiyalari PostgreSQL 17.11 ga smoke-test orqali qo‘llanib, Hibernate `validate` muvaffaqiyatli ishladi. iOS build Linux’da tekshirilmagan; buning uchun macOS/Xcode kerak. Bu MVP ishlab chiqarish muhiti uchun tayyor platforma deb hisoblanmaydi. SMS, push, to‘lov provayderi va haqiqiy IoT sensor integratsiyalari credentials hamda loyiha egasining tasdig‘ini kutmoqda.

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

### 2) Super Admin bootstrap

Dastlabki Super Admin raqamini maxfiy konfiguratsiyada `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE` orqali belgilang. Shu raqamning OTP’si muvaffaqiyatli tasdiqlanganda hisobga `SUPER_ADMIN` roli beriladi. Boshqa yangi foydalanuvchilar odatiy `USER` (oddiy foydalanuvchi) roli bilan ochiladi; investor yoki asalarichi rollarini vakolatli administrator tayinlaydi. Bootstrap raqamiga tegishli telefon/SMS xizmati loyiha egasi tomonidan xavfsiz saqlanishi shart.

Misol (faqat lokal shell):

```bash
export APP_SECURITY_BOOTSTRAP_ADMIN_PHONE=+998901234567
```

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
- Server role’lari: `USER`, `INVESTOR`, `BEEKEEPER`, `ADMIN`, `SUPER_ADMIN`; yangi hisoblar `USER` bo‘lib ochiladi, boshqa rollarni vakolatli administrator tayinlaydi. Admin endpointlari backend’da tekshiriladi.
- Development JWT secret, OTP pepper va DB paroli default qiymatlari **faqat lokal muhit** uchun. Production’da `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET_BASE64`, `OTP_PEPPER_BASE64`, CORS origin va boshqa maxfiy qiymatlarni secret manager/environment orqali belgilang.
- Birinchi real Adminni `APP_SECURITY_BOOTSTRAP_ADMIN_PHONE` bilan xavfsiz bootstrap qiling; oddiy administrator Super Admin rolini bera olmaydi.
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

Backend testlar o‘tdi; lokal PostgreSQL 17.11 da Flyway V1–V5 migratsiyalari qo‘llanib, Hibernate schema tekshiruvi muvaffaqiyatli yakunlandi. Ishlatilgan Flyway versiyasi PostgreSQL 17 uchun moslik ogohlantirishini beradi (rasmiy tekshirilgan eng so‘nggi major versiya 16); production’da tanlangan PostgreSQL/Flyway versiyalarini alohida tekshiring. Haqiqiy SMS, payment webhook va iOS release oqimlari credentials hamda tegishli build muhitida alohida tasdiqlanishi kerak.

## Railway joylash

Root `Dockerfile` web frontend va Spring Boot API’ni bitta Railway service’da ishga tushirish uchun tayyorlandi; kerakli Railway va PostgreSQL variable’lari `docs/RAILWAY_DEPLOY.md` da berilgan. Bu sozlama live deploy qilindi degani emas: Railway project/access, PostgreSQL service va haqiqiy SMS credentials kerak. Ular sozlanmaguncha ilova production-ready deb hisoblanmaydi.

## Hali egasi tasdiqlashi shart bo‘lganlar

Quyidagi ishlarni texnik taxmin bilan yoqib bo‘lmaydi: investitsiya va ari oilasiga egalik/ulushning huquqiy modeli; ROI, daromad formulasi yoki kafolat; komissiya va daromad taqsimoti; kasallik/yo‘qotish/force majeure xavfini kim o‘z zimmasiga olishi; muddatidan oldin chiqish/refund; KYC va moderatsiya siyosati; foydalanuvchi shartnomasi, privacy/consent; Click/Payme/Uzcard/Humo merchant contract; SMS/push credentials; IoT device protokoli va secret; operator aloqa ma’lumoti; hosting/backup va production domen. Bular tasdiqlanmaguncha haqiqiy pul olish, daromadni va’da qilish yoki sensor ma’lumotini avtomatik/verified deb atash mumkin emas.
